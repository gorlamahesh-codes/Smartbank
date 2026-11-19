package com.smartbank.service;

import com.smartbank.dao.BankDao;
import com.smartbank.dao.UserDao;
import com.smartbank.model.AccountInfo;
import com.smartbank.model.CustomerInfo;
import com.smartbank.model.DashboardStats;
import com.smartbank.model.SessionUser;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public class BankService {
    private final BankDao dao = new BankDao();
    private final UserDao users = new UserDao();

    // ─── Existing API (preserved) ───────────────────────────────────────────

    public String createAccount(long customerId, String type) throws SQLException {
        if (!type.equals("SAVINGS") && !type.equals("CURRENT")) throw new SQLException("Invalid account type");
        return dao.createAccount(customerId, type);
    }

    public void deposit(String account, BigDecimal amount, String actor) throws SQLException {
        dao.deposit(account, amount, actor);
    }

    public void withdraw(String account, BigDecimal amount, String actor) throws SQLException {
        dao.withdraw(account, amount, actor);
    }

    public void transfer(String from, String to, BigDecimal amount, String actor) throws SQLException {
        dao.transfer(from, to, amount, actor);
    }

    public String summary(String account) throws SQLException {
        return dao.accountSummary(account);
    }

    public String transactions(String account) throws SQLException {
        return dao.transactions(account);
    }

    public String adminStats() throws SQLException {
        return dao.adminStats();
    }

    public String customerStats(long userId) throws SQLException {
        return dao.customerStats(userId);
    }

    public String customerAccounts(long userId) throws SQLException {
        return dao.customerAccounts(userId);
    }

    public List<Object[]> customerTransactionRows(long userId) throws SQLException {
        return dao.customerTransactionRows(userId);
    }

    public List<Object[]> adminTransactionRows() throws SQLException {
        return dao.adminTransactionRows();
    }

    // ─── Role-aware helpers with ownership enforcement ──────────────────────

    public void depositAs(SessionUser session, String account, BigDecimal amount) throws SQLException {
        enforceAccountAccess(session, account);
        dao.deposit(account, amount, session.username());
    }

    public void withdrawAs(SessionUser session, String account, BigDecimal amount) throws SQLException {
        enforceAccountAccess(session, account);
        dao.withdraw(account, amount, session.username());
    }

    public void transferAs(SessionUser session, String from, String to, BigDecimal amount) throws SQLException {
        enforceAccountAccess(session, from); // source must be owned / admin
        dao.transfer(from, to, amount, session.username());
    }

    private void enforceAccountAccess(SessionUser session, String accountNumber) throws SQLException {
        if ("ADMIN".equalsIgnoreCase(session.role())) return;
        if (!dao.ownsAccount(session.userId(), accountNumber)) {
            throw new SQLException("You do not own this account. Operation denied.");
        }
    }

    // ─── Dashboard & management API ─────────────────────────────────────────

    public DashboardStats adminDashboard() throws SQLException {
        return dao.getAdminDashboardStats();
    }

    public DashboardStats customerDashboard(long userId) throws SQLException {
        return dao.getCustomerDashboardStats(userId);
    }

    public List<AccountInfo> customerAccountList(long userId) throws SQLException {
        return dao.listCustomerAccounts(userId);
    }

    public List<AccountInfo> allAccounts() throws SQLException {
        return dao.listAllAccounts();
    }

    public List<CustomerInfo> allCustomers() throws SQLException {
        return users.listAllCustomers();
    }

    public CustomerInfo customerProfile(long userId) throws SQLException {
        return users.findCustomerByUserId(userId);
    }

    public void updateProfile(long userId, String fullName, String email, String phone, String address) throws SQLException {
        users.updateCustomerProfile(userId, fullName, email, phone, address);
    }

    public List<Object[]> fraudAlerts() throws SQLException {
        return dao.listFraudAlerts();
    }

    public void setFraudAlertStatus(long alertId, String status) throws SQLException {
        dao.updateFraudAlertStatus(alertId, status);
    }

    public List<Object[]> auditLogs(int limit) throws SQLException {
        return dao.listAuditLogs(limit);
    }

    public List<Object[]> monthlySeries(Long userIdOrNull) throws SQLException {
        return dao.monthlyVolumeSeries(userIdOrNull);
    }

    public void setAccountStatus(SessionUser session, String accountNumber, String status) throws SQLException {
        if (!"ADMIN".equalsIgnoreCase(session.role())) throw new SQLException("Only administrators can change account status");
        dao.setAccountStatus(accountNumber, status, session.username());
    }

    public List<Object[]> customerGrowth() throws SQLException {
        return dao.customerGrowthSeries();
    }

    public boolean changePassword(String username, String currentPassword, String newPassword) throws SQLException {
        return users.changePassword(username, currentPassword, newPassword);
    }

    public void requestPasswordReset(String username) throws SQLException {
        users.requestPasswordReset(username);
    }
}
