package com.smartbank.ui.customer;

import com.smartbank.model.AccountInfo;
import com.smartbank.model.CustomerInfo;
import com.smartbank.model.DashboardStats;
import com.smartbank.model.SessionUser;
import com.smartbank.service.BankService;
import com.smartbank.ui.charts.BankCharts;
import com.smartbank.ui.components.MetricCard;
import com.smartbank.ui.components.NavSidebar;
import com.smartbank.ui.components.TablePanel;
import com.smartbank.ui.theme.UiTheme;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class CustomerHome extends JPanel {
    private static final NumberFormat INR = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
    private final SessionUser session;
    private final BankService bank;
    private final Runnable onLogout;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final NavSidebar nav;

    public CustomerHome(SessionUser session, BankService bank, Runnable onLogout) {
        super(new BorderLayout());
        this.session = session;
        this.bank = bank;
        this.onLogout = onLogout;
        setBackground(UiTheme.BG);
        String[] items = {"Overview", "My Accounts", "Deposit", "Withdraw", "Fund Transfer",
                "Transaction History", "My Profile", "Notifications", "Settings", "Logout"};
        nav = new NavSidebar("Customer", items, this::navigate);
        add(nav, BorderLayout.WEST);
        content.setOpaque(false);
        content.setBorder(new javax.swing.border.EmptyBorder(20, 24, 20, 24));
        add(content, BorderLayout.CENTER);
        navigate("Overview");
    }

    private void navigate(String item) {
        if ("Logout".equals(item)) {
            if (JOptionPane.showConfirmDialog(this, "Sign out of SmartBank?", "Confirm",
                    JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) onLogout.run();
            return;
        }
        nav.setActive(item);
        content.add(switch (item) {
            case "My Accounts" -> accountsPage();
            case "Deposit" -> moneyPage("deposit");
            case "Withdraw" -> moneyPage("withdraw");
            case "Fund Transfer" -> moneyPage("transfer");
            case "Transaction History" -> historyPage();
            case "My Profile" -> profilePage();
            case "Notifications" -> notesPage();
            case "Settings" -> settingsPage();
            default -> overview();
        }, item);
        cards.show(content, item);
        revalidate();
        repaint();
    }

    private JPanel overview() {
        JPanel page = UiTheme.page();
        page.add(header("Good to see you", displayName()), BorderLayout.NORTH);
        JPanel metrics = new JPanel(new GridLayout(1, 4, 12, 0));
        metrics.setOpaque(false);
        try {
            DashboardStats s = bank.customerDashboard(session.userId());
            metrics.add(new MetricCard("Total balance", money(s.totalBalance()), UiTheme.SUCCESS));
            metrics.add(new MetricCard("Accounts", String.valueOf(s.accountCount()), UiTheme.PRIMARY));
            metrics.add(new MetricCard("Monthly deposits", money(s.monthlyDeposits()), UiTheme.PRIMARY));
            metrics.add(new MetricCard("Monthly withdrawals", money(s.monthlyWithdrawals()), UiTheme.WARNING));
        } catch (Exception ex) {
            metrics.add(new MetricCard("Dashboard", "Unavailable", UiTheme.DANGER));
        }
        JPanel mid = new JPanel(new BorderLayout(0, 14));
        mid.setOpaque(false);
        mid.add(metrics, BorderLayout.NORTH);

        JPanel charts = new JPanel(new GridLayout(1, 2, 12, 0));
        charts.setOpaque(false);
        charts.setPreferredSize(new Dimension(0, 300));
        try {
            var series = bank.monthlySeries(session.userId());
            var txs = bank.customerTransactionRows(session.userId());
            charts.add(chartCard(BankCharts.depositsVsWithdrawals(series)));
            charts.add(chartCard(BankCharts.typePie(txs)));
        } catch (Exception ex) {
            charts.add(UiTheme.muted("Charts need a database connection."));
        }
        mid.add(charts, BorderLayout.CENTER);
        page.add(mid, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        for (String a : new String[]{"Deposit", "Withdraw", "Fund Transfer", "Transaction History"}) {
            JButton b = UiTheme.primary(a.equals("Transaction History") ? "View statement" : a);
            if (a.equals("Transaction History")) b = UiTheme.ghost("View statement");
            String target = a;
            b.addActionListener(e -> navigate(target));
            actions.add(b);
        }
        JButton refresh = UiTheme.ghost("Refresh");
        refresh.addActionListener(e -> navigate("Overview"));
        actions.add(refresh);
        page.add(actions, BorderLayout.SOUTH);
        return page;
    }

    private JPanel accountsPage() {
        JPanel page = UiTheme.page();
        page.add(header("My accounts", "Balances and status for accounts you own"), BorderLayout.NORTH);
        try {
            List<AccountInfo> list = bank.customerAccountList(session.userId());
            DefaultTableModel model = model(new String[]{"Account", "Type", "Balance", "Status", "Customer ID"});
            for (AccountInfo a : list) {
                model.addRow(new Object[]{a.accountNumber(), a.accountType(), money(a.balance()), a.status(), a.customerId()});
            }
            page.add(new TablePanel(model), BorderLayout.CENTER);
            if (list.isEmpty()) page.add(UiTheme.muted("No accounts yet. Ask an administrator to open one."), BorderLayout.SOUTH);
        } catch (Exception ex) {
            page.add(error(ex), BorderLayout.CENTER);
        }
        return page;
    }

    private JPanel moneyPage(String kind) {
        JPanel page = UiTheme.page();
        String title = switch (kind) {
            case "deposit" -> "Deposit money";
            case "withdraw" -> "Withdraw money";
            default -> "Fund transfer";
        };
        page.add(header(title, "Ownership is checked on the server before any balance changes"), BorderLayout.NORTH);
        JPanel form = UiTheme.card();
        JPanel grid = new JPanel(new GridLayout(0, 1, 8, 8));
        grid.setOpaque(false);
        JComboBox<String> from = new JComboBox<>();
        try {
            for (AccountInfo a : bank.customerAccountList(session.userId())) from.addItem(a.accountNumber());
        } catch (Exception ignored) { }
        JTextField to = UiTheme.field();
        JTextField amount = UiTheme.field();
        grid.add(new JLabel(kind.equals("transfer") ? "From account" : "Account"));
        grid.add(from);
        if (kind.equals("transfer")) {
            grid.add(new JLabel("Beneficiary account number"));
            grid.add(to);
        }
        grid.add(new JLabel("Amount (INR)"));
        grid.add(amount);
        JButton go = UiTheme.primary(title);
        go.addActionListener(e -> {
            try {
                if (from.getSelectedItem() == null) throw new Exception("You do not have an account yet.");
                BigDecimal value = new BigDecimal(amount.getText().trim()).setScale(2, RoundingMode.HALF_UP);
                String src = String.valueOf(from.getSelectedItem());
                int ok = JOptionPane.showConfirmDialog(this, title + " " + money(value) + "?", "Confirm", JOptionPane.OK_CANCEL_OPTION);
                if (ok != JOptionPane.OK_OPTION) return;
                switch (kind) {
                    case "deposit" -> bank.depositAs(session, src, value);
                    case "withdraw" -> bank.withdrawAs(session, src, value);
                    default -> bank.transferAs(session, src, to.getText().trim(), value);
                }
                UiTheme.toast(this, "Transaction completed.", false);
                amount.setText("");
            } catch (Exception ex) {
                UiTheme.toast(this, ex.getMessage() == null ? "Transaction failed" : ex.getMessage(), true);
            }
        });
        grid.add(go);
        form.add(grid, BorderLayout.NORTH);
        page.add(form, BorderLayout.CENTER);
        return page;
    }

    private JPanel historyPage() {
        JPanel page = UiTheme.page();
        page.add(header("Transaction history", "Search your own transactions"), BorderLayout.NORTH);
        try {
            DefaultTableModel model = txModel(bank.customerTransactionRows(session.userId()));
            JComboBox<String> type = new JComboBox<>(new String[]{"ALL", "DEPOSIT", "WITHDRAWAL", "TRANSFER"});
            JComboBox<String> status = new JComboBox<>(new String[]{"ALL", "SUCCESS", "FLAGGED", "FAILED"});
            TablePanel table = new TablePanel(model);
            type.addActionListener(e -> table.filterColumn(1, String.valueOf(type.getSelectedItem())));
            status.addActionListener(e -> table.filterColumn(3, String.valueOf(status.getSelectedItem())));
            JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT));
            filters.setOpaque(false);
            filters.add(new JLabel("Type"));
            filters.add(type);
            filters.add(new JLabel("Status"));
            filters.add(status);
            JPanel center = new JPanel(new BorderLayout(0, 8));
            center.setOpaque(false);
            center.add(filters, BorderLayout.NORTH);
            center.add(table, BorderLayout.CENTER);
            page.add(center, BorderLayout.CENTER);
        } catch (Exception ex) {
            page.add(error(ex), BorderLayout.CENTER);
        }
        return page;
    }

    private JPanel profilePage() {
        JPanel page = UiTheme.page();
        page.add(header("My profile", "Update the details stored on your customer record"), BorderLayout.NORTH);
        try {
            CustomerInfo info = bank.customerProfile(session.userId());
            if (info == null) {
                page.add(UiTheme.muted("No customer profile is linked to this login."), BorderLayout.CENTER);
                return page;
            }
            JTextField name = UiTheme.field(); name.setText(info.fullName());
            JTextField email = UiTheme.field(); email.setText(info.email());
            JTextField phone = UiTheme.field(); phone.setText(info.phone());
            JTextField address = UiTheme.field(); address.setText(info.address() == null ? "" : info.address());
            JPanel form = UiTheme.card();
            JPanel grid = new JPanel(new GridLayout(0, 2, 12, 10));
            grid.setOpaque(false);
            grid.add(new JLabel("Username")); grid.add(ro(info.username()));
            grid.add(new JLabel("Full name")); grid.add(name);
            grid.add(new JLabel("Email")); grid.add(email);
            grid.add(new JLabel("Phone")); grid.add(phone);
            grid.add(new JLabel("Address")); grid.add(address);
            JButton save = UiTheme.primary("Save profile");
            save.addActionListener(e -> {
                try {
                    bank.updateProfile(session.userId(), name.getText().trim(), email.getText().trim(),
                            phone.getText().trim(), address.getText().trim());
                    UiTheme.toast(this, "Profile saved.", false);
                } catch (Exception ex) {
                    UiTheme.toast(this, ex.getMessage(), true);
                }
            });
            form.add(grid, BorderLayout.CENTER);
            form.add(save, BorderLayout.SOUTH);
            page.add(form, BorderLayout.CENTER);
        } catch (Exception ex) {
            page.add(error(ex), BorderLayout.CENTER);
        }
        return page;
    }

    private JPanel notesPage() {
        JPanel page = UiTheme.page();
        page.add(header("Notifications", "Flagged activity on your accounts"), BorderLayout.NORTH);
        DefaultTableModel model = model(new String[]{"Reference", "Type", "Amount", "Status", "Date"});
        try {
            for (Object[] r : bank.customerTransactionRows(session.userId())) {
                if ("FLAGGED".equals(String.valueOf(r[3]))) {
                    model.addRow(new Object[]{r[0], r[1], r[2], r[3], r[4]});
                }
            }
        } catch (Exception ex) {
            return error(ex);
        }
        if (model.getRowCount() == 0) model.addRow(new Object[]{"—", "—", "—", "—", "No flagged notifications"});
        page.add(new TablePanel(model), BorderLayout.CENTER);
        return page;
    }

    private JPanel settingsPage() {
        JPanel page = UiTheme.page();
        page.add(header("Settings", "Change your password"), BorderLayout.NORTH);
        JPasswordField current = UiTheme.password();
        JPasswordField next = UiTheme.password();
        JPasswordField again = UiTheme.password();
        JPanel form = UiTheme.card();
        JPanel grid = new JPanel(new GridLayout(0, 1, 8, 8));
        grid.setOpaque(false);
        grid.add(new JLabel("Current password")); grid.add(current);
        grid.add(new JLabel("New password")); grid.add(next);
        grid.add(new JLabel("Confirm new password")); grid.add(again);
        JButton save = UiTheme.primary("Update password");
        save.addActionListener(e -> {
            String n = new String(next.getPassword());
            if (n.length() < 8 || !n.equals(new String(again.getPassword()))) {
                UiTheme.toast(this, "New passwords must match and be at least 8 characters.", true);
                return;
            }
            try {
                boolean ok = bank.changePassword(session.username(), new String(current.getPassword()), n);
                UiTheme.toast(this, ok ? "Password updated." : "Current password is incorrect.", !ok);
            } catch (Exception ex) {
                UiTheme.toast(this, ex.getMessage(), true);
            }
        });
        grid.add(save);
        form.add(grid, BorderLayout.NORTH);
        page.add(form, BorderLayout.CENTER);
        return page;
    }

    private JPanel header(String title, String subtitle) {
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel h = UiTheme.heading(title);
        JLabel date = UiTheme.muted(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")) + "   ·   " + subtitle);
        top.add(h, BorderLayout.NORTH);
        top.add(date, BorderLayout.SOUTH);
        return top;
    }

    private String displayName() {
        try {
            CustomerInfo info = bank.customerProfile(session.userId());
            if (info != null && info.fullName() != null) return info.fullName();
        } catch (Exception ignored) { }
        return session.username();
    }

    private static JComponent chartCard(JComponent chart) {
        JPanel p = UiTheme.card();
        p.add(chart, BorderLayout.CENTER);
        return p;
    }

    public static DefaultTableModel model(String[] cols) {
        return new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    public static DefaultTableModel txModel(List<Object[]> rows) {
        DefaultTableModel model = model(new String[]{"Reference", "Type", "Amount", "Status", "Date", "From", "To"});
        for (Object[] r : rows) model.addRow(r);
        return model;
    }

    private static JTextField ro(String v) {
        JTextField f = UiTheme.field();
        f.setText(v);
        f.setEditable(false);
        return f;
    }

    private JPanel error(Exception ex) {
        JPanel p = UiTheme.page();
        p.add(UiTheme.muted(ex.getMessage() == null ? "Something went wrong" : ex.getMessage()), BorderLayout.NORTH);
        return p;
    }

    public static String money(BigDecimal v) {
        return v == null ? "₹0.00" : INR.format(v);
    }
}
