package com.smartbank.dao;

import com.smartbank.config.DBConnection;
import com.smartbank.model.CustomerInfo;
import com.smartbank.model.SessionUser;
import com.smartbank.security.PasswordUtil;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    public void register(String username, String password, String fullName, String email,
                         String phone, String address) throws SQLException {
        String userSql = "INSERT INTO users(username,password_hash,role) VALUES(?,?,'CUSTOMER')";
        String customerSql = "INSERT INTO customers(user_id,full_name,email,phone,address) VALUES(?,?,?,?,?)";
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                long userId;
                try (PreparedStatement ps = con.prepareStatement(userSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, username);
                    ps.setString(2, PasswordUtil.hash(password));
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (!rs.next()) throw new SQLException("Could not create user");
                        userId = rs.getLong(1);
                    }
                }
                try (PreparedStatement ps = con.prepareStatement(customerSql)) {
                    ps.setLong(1, userId);
                    ps.setString(2, fullName);
                    ps.setString(3, email);
                    ps.setString(4, phone);
                    ps.setString(5, address);
                    ps.executeUpdate();
                }
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    public SessionUser login(String username, String password) throws SQLException {
        String sql = "SELECT user_id,username,password_hash,role FROM users WHERE username=? AND active=TRUE";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || !PasswordUtil.verify(password, rs.getString("password_hash"))) return null;
                return new SessionUser(rs.getLong("user_id"), rs.getString("username"), rs.getString("role"));
            }
        }
    }

    public CustomerInfo findCustomerByUserId(long userId) throws SQLException {
        String sql = """
            SELECT c.customer_id, c.user_id, c.full_name, c.email, c.phone, c.address,
                   u.username, u.role
            FROM customers c JOIN users u ON u.user_id = c.user_id
            WHERE c.user_id = ?
            """;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new CustomerInfo(
                        rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8));
            }
        }
    }

    public List<CustomerInfo> listAllCustomers() throws SQLException {
        String sql = """
            SELECT c.customer_id, c.user_id, c.full_name, c.email, c.phone, c.address,
                   u.username, u.role
            FROM customers c JOIN users u ON u.user_id = c.user_id
            ORDER BY c.created_at DESC
            """;
        List<CustomerInfo> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new CustomerInfo(
                        rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8)));
            }
        }
        return list;
    }

    public void updateCustomerProfile(long userId, String fullName, String email, String phone, String address) throws SQLException {
        String sql = "UPDATE customers SET full_name=?, email=?, phone=?, address=? WHERE user_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, email);
            ps.setString(3, phone);
            ps.setString(4, address);
            ps.setLong(5, userId);
            if (ps.executeUpdate() == 0) throw new SQLException("Profile not found");
        }
    }

    public boolean changePassword(String username, String currentPassword, String newPassword) throws SQLException {
        String sql = "SELECT password_hash FROM users WHERE username=? AND active=TRUE";
        try (Connection con = DBConnection.getConnection()) {
            String hash;
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return false;
                    hash = rs.getString(1);
                }
            }
            if (!PasswordUtil.verify(currentPassword, hash)) return false;
            try (PreparedStatement ps = con.prepareStatement("UPDATE users SET password_hash=? WHERE username=?")) {
                ps.setString(1, PasswordUtil.hash(newPassword));
                ps.setString(2, username);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO audit_logs(username,action,details) VALUES(?,?,?)")) {
                ps.setString(1, username);
                ps.setString(2, "PASSWORD_CHANGE");
                ps.setString(3, "Customer changed password");
                ps.executeUpdate();
            }
            return true;
        }
    }

    /** Logs a reset request without revealing whether the username exists to the caller. */
    public void requestPasswordReset(String username) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement check = con.prepareStatement(sql)) {
            check.setString(1, username);
            try (ResultSet rs = check.executeQuery()) {
                if (!rs.next()) return;
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO audit_logs(username,action,details) VALUES(?,?,?)")) {
                ps.setString(1, username);
                ps.setString(2, "PASSWORD_RESET_REQUEST");
                ps.setString(3, "User requested password assistance from the login screen");
                ps.executeUpdate();
            }
        }
    }
}
