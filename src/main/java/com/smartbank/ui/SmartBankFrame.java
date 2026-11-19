package com.smartbank.ui;

import com.smartbank.dao.UserDao;
import com.smartbank.service.BankService;
import com.smartbank.ui.admin.AdminHome;
import com.smartbank.ui.auth.LoginView;
import com.smartbank.ui.auth.RegisterView;
import com.smartbank.ui.customer.CustomerHome;
import com.smartbank.ui.theme.UiTheme;
import java.awt.Dimension;
import java.util.prefs.Preferences;
import javax.swing.*;

public class SmartBankFrame extends JFrame {
    private final UserDao users = new UserDao();
    private final BankService bank = new BankService();
    private final Preferences prefs = Preferences.userNodeForPackage(SmartBankFrame.class);
    private boolean rememberUser = true;

    public SmartBankFrame() {
        setTitle("SmartBank");
        setSize(1280, 820);
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        showLogin();
    }

    private void showLogin() {
        String remembered = prefs.get("username", "");
        setContentPane(new LoginView(remembered,
                this::login,
                this::showRegister,
                this::forgot,
                remember -> rememberUser = remember));
        revalidate();
        repaint();
    }

    private void login(String username, String password) {
        if (username.isBlank() || password.isBlank()) {
            UiTheme.toast(this, "Enter both username and password.", true);
            return;
        }
        setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
        try {
            var session = users.login(username, password);
            if (session == null) {
                UiTheme.toast(this, "Invalid username or password.", true);
                return;
            }
            if (rememberUser) prefs.put("username", username);
            else prefs.remove("username");
            if ("ADMIN".equalsIgnoreCase(session.role())) {
                setContentPane(new AdminHome(session, bank, this::showLogin));
            } else {
                setContentPane(new CustomerHome(session, bank, this::showLogin));
            }
            revalidate();
            repaint();
        } catch (Exception ex) {
            UiTheme.toast(this, ex.getMessage() == null ? "Unable to sign in. Check MySQL and db.properties." : ex.getMessage(), true);
        } finally {
            setCursor(java.awt.Cursor.getDefaultCursor());
        }
    }

    private void forgot(String username) {
        if (username == null || username.isBlank()) {
            UiTheme.toast(this, "Enter your username first, then choose Forgot password.", true);
            return;
        }
        try {
            bank.requestPasswordReset(username.trim());
        } catch (Exception ignored) { }
        UiTheme.toast(this, "If that username exists, the request is recorded for an administrator. SmartBank does not email passwords.", false);
    }

    private void showRegister() {
        setContentPane(new RegisterView(reg -> {
            try {
                users.register(reg.username(), reg.password(), reg.fullName(), reg.email(), reg.phone(), reg.address());
                UiTheme.toast(this, "Account created. Sign in to continue.", false);
                showLogin();
            } catch (Exception ex) {
                UiTheme.toast(this, ex.getMessage() == null ? "Registration failed" : ex.getMessage(), true);
            }
        }, this::showLogin));
        revalidate();
        repaint();
    }
}
