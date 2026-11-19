package com.smartbank.dao;

import com.smartbank.config.DBConnection;
import com.smartbank.model.AccountInfo;
import com.smartbank.model.DashboardStats;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BankDao {
    private static final BigDecimal FRAUD_LIMIT = new BigDecimal("50000.00");

    // ─── Existing core operations (preserved) ───────────────────────────────

    public String createAccount(long customerId, String type) throws SQLException {
        String number = "SB" + System.currentTimeMillis();
        String sql = "INSERT INTO accounts(customer_id,account_number,account_type) VALUES(?,?,?)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, customerId);
            p.setString(2, number);
            p.setString(3, type);
            p.executeUpdate();
        }
        return number;
    }

    public void deposit(String accountNo, BigDecimal amount, String actor) throws SQLException {
        validateAmount(amount);
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                long accountId = lockAccount(c, accountNo);
                ensureActive(c, accountId);
                try (PreparedStatement p = c.prepareStatement("UPDATE accounts SET balance=balance+? WHERE account_id=?")) {
                    p.setBigDecimal(1, amount);
                    p.setLong(2, accountId);
                    p.executeUpdate();
                }
                record(c, null, accountId, "DEPOSIT", amount,
                        amount.compareTo(FRAUD_LIMIT) >= 0 ? "FLAGGED" : "SUCCESS", "Cash deposit");
                if (amount.compareTo(FRAUD_LIMIT) >= 0) alert(c, accountId, amount, "Large deposit requires review");
                audit(c, actor, "DEPOSIT", accountNo + " amount " + amount);
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e instanceof SQLException se ? se : new SQLException(e);
            }
        }
    }

    public void withdraw(String accountNo, BigDecimal amount, String actor) throws SQLException {
        validateAmount(amount);
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                long id = lockAccount(c, accountNo);
                ensureActive(c, id);
                BigDecimal balance = balance(c, id);
                if (balance.compareTo(amount) < 0) throw new SQLException("Insufficient balance");
                try (PreparedStatement p = c.prepareStatement("UPDATE accounts SET balance=balance-? WHERE account_id=?")) {
                    p.setBigDecimal(1, amount);
                    p.setLong(2, id);
                    p.executeUpdate();
                }
                boolean flagged = amount.compareTo(FRAUD_LIMIT) >= 0;
                long tx = record(c, id, null, "WITHDRAWAL", amount, flagged ? "FLAGGED" : "SUCCESS", "Cash withdrawal");
                if (flagged) alert(c, id, amount, "Large withdrawal requires review");
                audit(c, actor, "WITHDRAWAL", accountNo + " amount " + amount);
                c.commit();
                if (flagged) throw new SQLException("Transaction processed but flagged for administrator review. Reference: " + tx);
            } catch (Exception e) {
                if (!c.getAutoCommit() && (e.getMessage() == null || !e.getMessage().startsWith("Transaction processed but flagged"))) {
                    c.rollback();
                }
                if (e instanceof SQLException se) throw se;
                throw new SQLException(e);
            }
        }
    }

    public void transfer(String fromNo, String toNo, BigDecimal amount, String actor) throws SQLException {
        validateAmount(amount);
        if (fromNo.equals(toNo)) throw new SQLException("Source and destination accounts must differ");
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                String first = fromNo.compareTo(toNo) < 0 ? fromNo : toNo;
                String second = fromNo.compareTo(toNo) < 0 ? toNo : fromNo;
                long firstId = lockAccount(c, first);
                long secondId = lockAccount(c, second);
                long fromId = fromNo.equals(first) ? firstId : secondId;
                long toId = toNo.equals(first) ? firstId : secondId;
                ensureActive(c, fromId);
                ensureActive(c, toId);
                if (balance(c, fromId).compareTo(amount) < 0) throw new SQLException("Insufficient balance");
                try (PreparedStatement p = c.prepareStatement("UPDATE accounts SET balance=balance-? WHERE account_id=?")) {
                    p.setBigDecimal(1, amount);
                    p.setLong(2, fromId);
                    p.executeUpdate();
                }
                try (PreparedStatement p = c.prepareStatement("UPDATE accounts SET balance=balance+? WHERE account_id=?")) {
                    p.setBigDecimal(1, amount);
                    p.setLong(2, toId);
                    p.executeUpdate();
                }
                boolean flagged = amount.compareTo(FRAUD_LIMIT) >= 0;
                long tx = record(c, fromId, toId, "TRANSFER", amount, flagged ? "FLAGGED" : "SUCCESS", "Account transfer");
                if (flagged) alert(c, fromId, amount, "Large transfer requires review");
                audit(c, actor, "TRANSFER", fromNo + " -> " + toNo + " amount " + amount);
                c.commit();
                if (flagged) throw new SQLException("Transfer processed but flagged for administrator review. Reference: " + tx);
            } catch (Exception e) {
                if (!c.getAutoCommit() && (e.getMessage() == null || !e.getMessage().startsWith("Transfer processed but flagged"))) {
                    c.rollback();
                }
                if (e instanceof SQLException se) throw se;
                throw new SQLException(e);
            }
        }
    }

    public String accountSummary(String accountNo) throws SQLException {
        String sql = "SELECT c.full_name,a.account_number,a.account_type,a.balance,a.status FROM accounts a JOIN customers c ON a.customer_id=c.customer_id WHERE a.account_number=?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, accountNo);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) return "Account not found";
                return "Customer: " + r.getString(1) + "\nAccount: " + r.getString(2) +
                       "\nType: " + r.getString(3) + "\nBalance: ₹" + r.getBigDecimal(4) +
                       "\nStatus: " + r.getString(5);
            }
        }
    }

    public String transactions(String accountNo) throws SQLException {
        String sql = "SELECT t.reference_no,t.transaction_type,t.amount,t.status,t.created_at FROM transactions t " +
          "JOIN accounts a ON (a.account_id=t.from_account OR a.account_id=t.to_account) " +
          "WHERE a.account_number=? ORDER BY t.created_at DESC LIMIT 100";
        StringBuilder out = new StringBuilder("REFERENCE | TYPE | AMOUNT | STATUS | DATE\n\n");
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, accountNo);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) out.append(r.getString(1)).append(" | ").append(r.getString(2)).append(" | ₹")
                  .append(r.getBigDecimal(3)).append(" | ").append(r.getString(4)).append(" | ")
                  .append(r.getTimestamp(5)).append("\n");
            }
        }
        return out.toString();
    }

    public String adminStats() throws SQLException {
        String sql = "SELECT (SELECT COUNT(*) FROM customers), (SELECT COUNT(*) FROM accounts), " +
                "(SELECT COALESCE(SUM(balance),0) FROM accounts), " +
                "(SELECT COUNT(*) FROM transactions), (SELECT COUNT(*) FROM fraud_alerts WHERE alert_status='OPEN')";
        try (Connection c = DBConnection.getConnection(); Statement st = c.createStatement(); ResultSet r = st.executeQuery(sql)) {
            r.next();
            return "Customers: " + r.getLong(1) + "\nAccounts: " + r.getLong(2) +
                    "\nTotal deposits held: ₹" + r.getBigDecimal(3) + "\nTransactions: " + r.getLong(4) +
                    "\nOpen fraud alerts: " + r.getLong(5);
        }
    }

    public String customerStats(long userId) throws SQLException {
        String sql = "SELECT COUNT(a.account_id), COALESCE(SUM(a.balance),0) FROM customers c " +
                "LEFT JOIN accounts a ON a.customer_id=c.customer_id WHERE c.user_id=?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, userId);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) return "No customer profile found";
                return "Your accounts: " + r.getLong(1) + "\nTotal balance: ₹" + r.getBigDecimal(2);
            }
        }
    }

    public String customerAccounts(long userId) throws SQLException {
        String sql = "SELECT a.account_number,a.account_type,a.balance,a.status FROM accounts a " +
                "JOIN customers c ON c.customer_id=a.customer_id WHERE c.user_id=? ORDER BY a.created_at DESC";
        StringBuilder b = new StringBuilder("ACCOUNT NUMBER | TYPE | BALANCE | STATUS\n\n");
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, userId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) b.append(r.getString(1)).append(" | ").append(r.getString(2)).append(" | ₹")
                    .append(r.getBigDecimal(3)).append(" | ").append(r.getString(4)).append("\n");
            }
        }
        return b.toString();
    }

    public List<Object[]> customerTransactionRows(long userId) throws SQLException {
        String sql = "SELECT t.reference_no,t.transaction_type,t.amount,t.status,t.created_at," +
                "COALESCE(f.account_number,''),COALESCE(toa.account_number,'') " +
                "FROM transactions t " +
                "LEFT JOIN accounts f ON f.account_id=t.from_account " +
                "LEFT JOIN accounts toa ON toa.account_id=t.to_account " +
                "WHERE EXISTS (SELECT 1 FROM customers c JOIN accounts owned ON owned.customer_id=c.customer_id " +
                "WHERE c.user_id=? AND (t.from_account=owned.account_id OR t.to_account=owned.account_id)) " +
                "ORDER BY t.created_at DESC LIMIT 200";
        List<Object[]> rows = new ArrayList<>();
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, userId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) rows.add(new Object[]{r.getString(1), r.getString(2), r.getBigDecimal(3),
                        r.getString(4), r.getTimestamp(5), r.getString(6), r.getString(7)});
            }
        }
        return rows;
    }

    public List<Object[]> adminTransactionRows() throws SQLException {
        String sql = "SELECT t.reference_no,t.transaction_type,t.amount,t.status,t.created_at," +
                "COALESCE(f.account_number,''),COALESCE(toa.account_number,'') " +
                "FROM transactions t LEFT JOIN accounts f ON f.account_id=t.from_account " +
                "LEFT JOIN accounts toa ON toa.account_id=t.to_account " +
                "ORDER BY t.created_at DESC LIMIT 300";
        List<Object[]> rows = new ArrayList<>();
        try (Connection c = DBConnection.getConnection(); Statement st = c.createStatement();
             ResultSet r = st.executeQuery(sql)) {
            while (r.next()) rows.add(new Object[]{r.getString(1), r.getString(2), r.getBigDecimal(3),
                    r.getString(4), r.getTimestamp(5), r.getString(6), r.getString(7)});
        }
        return rows;
    }

    // ─── New / enhanced methods for professional dashboards ─────────────────

    public DashboardStats getAdminDashboardStats() throws SQLException {
        String sql = """
            SELECT
              (SELECT COUNT(*) FROM customers),
              (SELECT COUNT(*) FROM accounts),
              (SELECT COUNT(*) FROM accounts WHERE status='ACTIVE'),
              (SELECT COALESCE(SUM(balance),0) FROM accounts),
              (SELECT COALESCE(SUM(amount),0) FROM transactions WHERE transaction_type='DEPOSIT'
                 AND created_at >= DATE_FORMAT(NOW(),'%Y-%m-01')),
              (SELECT COALESCE(SUM(amount),0) FROM transactions WHERE transaction_type='WITHDRAWAL'
                 AND created_at >= DATE_FORMAT(NOW(),'%Y-%m-01')),
              (SELECT COUNT(*) FROM transactions),
              (SELECT COUNT(*) FROM fraud_alerts WHERE alert_status='OPEN')
            """;
        try (Connection c = DBConnection.getConnection(); Statement st = c.createStatement();
             ResultSet r = st.executeQuery(sql)) {
            r.next();
            return new DashboardStats(
                    r.getLong(1), r.getLong(2), r.getLong(3), r.getBigDecimal(4),
                    r.getBigDecimal(5), r.getBigDecimal(6), r.getLong(7), r.getLong(8));
        }
    }

    public DashboardStats getCustomerDashboardStats(long userId) throws SQLException {
        String sql = """
            SELECT
              COUNT(DISTINCT a.account_id),
              COUNT(DISTINCT CASE WHEN a.status='ACTIVE' THEN a.account_id END),
              COALESCE(SUM(a.balance),0),
              (SELECT COALESCE(SUM(t.amount),0) FROM transactions t
                 JOIN accounts oa ON (oa.account_id=t.to_account OR oa.account_id=t.from_account)
                 JOIN customers oc ON oc.customer_id=oa.customer_id
                 WHERE oc.user_id=? AND t.transaction_type='DEPOSIT'
                   AND t.created_at >= DATE_FORMAT(NOW(),'%Y-%m-01')),
              (SELECT COALESCE(SUM(t.amount),0) FROM transactions t
                 JOIN accounts oa ON (oa.account_id=t.from_account)
                 JOIN customers oc ON oc.customer_id=oa.customer_id
                 WHERE oc.user_id=? AND t.transaction_type='WITHDRAWAL'
                   AND t.created_at >= DATE_FORMAT(NOW(),'%Y-%m-01')),
              (SELECT COUNT(*) FROM transactions t
                 JOIN accounts oa ON (oa.account_id=t.from_account OR oa.account_id=t.to_account)
                 JOIN customers oc ON oc.customer_id=oa.customer_id
                 WHERE oc.user_id=?)
            FROM customers c
            LEFT JOIN accounts a ON a.customer_id=c.customer_id
            WHERE c.user_id=?
            """;
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, userId);
            p.setLong(2, userId);
            p.setLong(3, userId);
            p.setLong(4, userId);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) return DashboardStats.empty();
                return new DashboardStats(
                        0, r.getLong(1), r.getLong(2), r.getBigDecimal(3),
                        r.getBigDecimal(4), r.getBigDecimal(5), r.getLong(6), 0);
            }
        }
    }

    public List<AccountInfo> listCustomerAccounts(long userId) throws SQLException {
        String sql = """
            SELECT a.account_id, a.account_number, a.account_type, a.balance, a.status, a.customer_id
            FROM accounts a JOIN customers c ON c.customer_id=a.customer_id
            WHERE c.user_id=? ORDER BY a.created_at DESC
            """;
        List<AccountInfo> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, userId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    list.add(new AccountInfo(r.getLong(1), r.getString(2), r.getString(3),
                            r.getBigDecimal(4), r.getString(5), r.getLong(6)));
                }
            }
        }
        return list;
    }

    public List<AccountInfo> listAllAccounts() throws SQLException {
        String sql = """
            SELECT a.account_id, a.account_number, a.account_type, a.balance, a.status, a.customer_id
            FROM accounts a ORDER BY a.created_at DESC LIMIT 500
            """;
        List<AccountInfo> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection(); Statement st = c.createStatement();
             ResultSet r = st.executeQuery(sql)) {
            while (r.next()) {
                list.add(new AccountInfo(r.getLong(1), r.getString(2), r.getString(3),
                        r.getBigDecimal(4), r.getString(5), r.getLong(6)));
            }
        }
        return list;
    }

    /** Returns true if the given account belongs to the user (customer ownership check). */
    public boolean ownsAccount(long userId, String accountNumber) throws SQLException {
        String sql = """
            SELECT 1 FROM accounts a
            JOIN customers c ON c.customer_id=a.customer_id
            WHERE c.user_id=? AND a.account_number=?
            """;
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, userId);
            p.setString(2, accountNumber);
            try (ResultSet r = p.executeQuery()) {
                return r.next();
            }
        }
    }

    public List<Object[]> listFraudAlerts() throws SQLException {
        String sql = """
            SELECT fa.alert_id, fa.risk_score, fa.reason, fa.alert_status, fa.created_at,
                   a.account_number, t.reference_no, t.amount
            FROM fraud_alerts fa
            JOIN accounts a ON a.account_id=fa.account_id
            JOIN transactions t ON t.transaction_id=fa.transaction_id
            ORDER BY fa.created_at DESC LIMIT 200
            """;
        List<Object[]> rows = new ArrayList<>();
        try (Connection c = DBConnection.getConnection(); Statement st = c.createStatement();
             ResultSet r = st.executeQuery(sql)) {
            while (r.next()) {
                rows.add(new Object[]{
                        r.getLong(1), r.getInt(2), r.getString(3), r.getString(4),
                        r.getTimestamp(5), r.getString(6), r.getString(7), r.getBigDecimal(8)
                });
            }
        }
        return rows;
    }

    public void updateFraudAlertStatus(long alertId, String status) throws SQLException {
        if (!status.equals("OPEN") && !status.equals("REVIEWED") && !status.equals("CLEARED")) {
            throw new SQLException("Invalid alert status");
        }
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement("UPDATE fraud_alerts SET alert_status=? WHERE alert_id=?")) {
            p.setString(1, status);
            p.setLong(2, alertId);
            if (p.executeUpdate() == 0) throw new SQLException("Alert not found");
        }
    }

    public List<Object[]> listAuditLogs(int limit) throws SQLException {
        String sql = "SELECT log_id, username, action, details, created_at FROM audit_logs ORDER BY created_at DESC LIMIT ?";
        List<Object[]> rows = new ArrayList<>();
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, Math.min(limit, 500));
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    rows.add(new Object[]{r.getLong(1), r.getString(2), r.getString(3),
                            r.getString(4), r.getTimestamp(5)});
                }
            }
        }
        return rows;
    }

    public void setAccountStatus(String accountNumber, String status, String actor) throws SQLException {
        if (!"ACTIVE".equals(status) && !"FROZEN".equals(status) && !"CLOSED".equals(status)) {
            throw new SQLException("Invalid account status");
        }
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement p = c.prepareStatement(
                        "UPDATE accounts SET status=? WHERE account_number=?")) {
                    p.setString(1, status);
                    p.setString(2, accountNumber);
                    if (p.executeUpdate() == 0) throw new SQLException("Account not found");
                }
                audit(c, actor, "ACCOUNT_STATUS", accountNumber + " -> " + status);
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public List<Object[]> customerGrowthSeries() throws SQLException {
        String sql = """
            SELECT DATE_FORMAT(created_at,'%Y-%m') ym, COUNT(*)
            FROM customers
            WHERE created_at >= DATE_SUB(DATE_FORMAT(NOW(),'%Y-%m-01'), INTERVAL 5 MONTH)
            GROUP BY ym ORDER BY ym
            """;
        List<Object[]> rows = new ArrayList<>();
        try (Connection c = DBConnection.getConnection(); Statement st = c.createStatement();
             ResultSet r = st.executeQuery(sql)) {
            while (r.next()) rows.add(new Object[]{r.getString(1), r.getLong(2)});
        }
        return rows;
    }

    /** Monthly volume for the last 6 months (for simple charts). Returns [monthLabel, deposits, withdrawals]. */
    public List<Object[]> monthlyVolumeSeries(Long userIdOrNull) throws SQLException {
        String filter = userIdOrNull == null ? "" :
                " AND EXISTS (SELECT 1 FROM accounts oa JOIN customers oc ON oc.customer_id=oa.customer_id " +
                "WHERE oc.user_id=? AND (t.from_account=oa.account_id OR t.to_account=oa.account_id))";
        String sql = """
            SELECT DATE_FORMAT(t.created_at,'%Y-%m') AS ym,
                   COALESCE(SUM(CASE WHEN t.transaction_type='DEPOSIT' THEN t.amount ELSE 0 END),0),
                   COALESCE(SUM(CASE WHEN t.transaction_type='WITHDRAWAL' THEN t.amount ELSE 0 END),0)
            FROM transactions t
            WHERE t.created_at >= DATE_SUB(DATE_FORMAT(NOW(),'%Y-%m-01'), INTERVAL 5 MONTH)
            """ + filter + """
            GROUP BY ym ORDER BY ym
            """;
        List<Object[]> rows = new ArrayList<>();
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            if (userIdOrNull != null) p.setLong(1, userIdOrNull);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    rows.add(new Object[]{r.getString(1), r.getBigDecimal(2), r.getBigDecimal(3)});
                }
            }
        }
        return rows;
    }

    // ─── Private helpers (unchanged) ────────────────────────────────────────

    private static void validateAmount(BigDecimal a) throws SQLException {
        if (a == null || a.compareTo(BigDecimal.ZERO) <= 0 || a.scale() > 2)
            throw new SQLException("Amount must be positive and have at most 2 decimal places");
    }

    private static long lockAccount(Connection c, String no) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("SELECT account_id FROM accounts WHERE account_number=? FOR UPDATE")) {
            p.setString(1, no);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) throw new SQLException("Account not found: " + no);
                return r.getLong(1);
            }
        }
    }

    private static void ensureActive(Connection c, long id) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("SELECT status FROM accounts WHERE account_id=?")) {
            p.setLong(1, id);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next() || !"ACTIVE".equals(r.getString(1))) throw new SQLException("Account is not active");
            }
        }
    }

    private static BigDecimal balance(Connection c, long id) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("SELECT balance FROM accounts WHERE account_id=?")) {
            p.setLong(1, id);
            try (ResultSet r = p.executeQuery()) {
                r.next();
                return r.getBigDecimal(1);
            }
        }
    }

    private static long record(Connection c, Long from, Long to, String type, BigDecimal amount,
                               String status, String desc) throws SQLException {
        String sql = "INSERT INTO transactions(reference_no,from_account,to_account,transaction_type,amount,status,description) VALUES(?,?,?,?,?,?,?)";
        try (PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            p.setString(1, UUID.randomUUID().toString());
            if (from == null) p.setNull(2, Types.BIGINT); else p.setLong(2, from);
            if (to == null) p.setNull(3, Types.BIGINT); else p.setLong(3, to);
            p.setString(4, type);
            p.setBigDecimal(5, amount);
            p.setString(6, status);
            p.setString(7, desc);
            p.executeUpdate();
            try (ResultSet r = p.getGeneratedKeys()) {
                r.next();
                return r.getLong(1);
            }
        }
    }

    private static void alert(Connection c, long accountId, BigDecimal amount, String reason) throws SQLException {
        String sql = "INSERT INTO fraud_alerts(transaction_id,account_id,risk_score,reason) VALUES((SELECT MAX(transaction_id) FROM transactions),?,?,?)";
        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, accountId);
            p.setInt(2, amount.compareTo(new BigDecimal("100000")) >= 0 ? 90 : 70);
            p.setString(3, reason);
            p.executeUpdate();
        }
    }

    private static void audit(Connection c, String user, String action, String details) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("INSERT INTO audit_logs(username,action,details) VALUES(?,?,?)")) {
            p.setString(1, user);
            p.setString(2, action);
            p.setString(3, details);
            p.executeUpdate();
        }
    }
}
