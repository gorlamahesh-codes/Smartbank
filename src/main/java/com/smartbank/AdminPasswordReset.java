
package com.smartbank;

import com.smartbank.config.DBConnection;
import com.smartbank.security.PasswordUtil;

import javax.swing.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Arrays;

public class AdminPasswordReset {

    public static void main(String[] args) {

        JPasswordField passwordField = new JPasswordField(20);

        int option = JOptionPane.showConfirmDialog(
            null,
            passwordField,
            "Enter New Admin Password",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );

        if (option != JOptionPane.OK_OPTION) {
            return;
        }

        char[] passwordChars = passwordField.getPassword();

        try {
            String newPassword = new String(passwordChars);

            if (newPassword.length() < 4) {
                JOptionPane.showMessageDialog(
                    null,
                    "Password must contain at least 12 characters."
                );
                return;
            }

            // Generate BCrypt hash using existing project utility
            String hashedPassword = PasswordUtil.hash(newPassword);

            String sql = """
                UPDATE users
                SET password_hash = ?
                WHERE username = ?
                  AND role = 'ADMIN'
                  AND active = TRUE
                """;

            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setString(1, hashedPassword);
                ps.setString(2, "root");

                int rows = ps.executeUpdate();

                if (rows == 1) {
                    JOptionPane.showMessageDialog(
                        null,
                        "Admin password reset successfully!\n\n" +
                        "Username: root\n" +
                        "Role: ADMIN\n" +
                        "You can now log in."
                    );
                } else {
                    JOptionPane.showMessageDialog(
                        null,
                        "Admin account not found or inactive."
                    );
                }
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                null,
                "Password reset failed:\n" + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
            e.printStackTrace();

        } finally {
            Arrays.fill(passwordChars, '\0');
        }
    }
}
