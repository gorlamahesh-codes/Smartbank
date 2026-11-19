package com.smartbank.ui.admin;

import com.smartbank.model.AccountInfo;
import com.smartbank.model.DashboardStats;
import com.smartbank.model.SessionUser;
import com.smartbank.service.BankService;
import com.smartbank.ui.charts.BankCharts;
import com.smartbank.ui.components.MetricCard;
import com.smartbank.ui.components.NavSidebar;
import com.smartbank.ui.components.TablePanel;
import com.smartbank.ui.customer.CustomerHome;
import com.smartbank.ui.theme.UiTheme;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class AdminHome extends JPanel {
    private final SessionUser session;
    private final BankService bank;
    private final Runnable onLogout;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final NavSidebar nav;

    public AdminHome(SessionUser session, BankService bank, Runnable onLogout) {
        super(new BorderLayout());
        this.session = session;
        this.bank = bank;
        this.onLogout = onLogout;
        setBackground(UiTheme.BG);
        String[] items = {"Admin Overview", "Customer Management", "Account Management", "Transaction Management",
                "Deposit Management", "Withdrawal Management", "Fund Transfers", "Fraud Alerts",
                "Audit Logs", "Admin Profile", "Settings", "Logout"};
        nav = new NavSidebar("Administrator", items, this::navigate);
        add(nav, BorderLayout.WEST);
        content.setOpaque(false);
        content.setBorder(new javax.swing.border.EmptyBorder(20, 24, 20, 24));
        add(content, BorderLayout.CENTER);
        navigate("Admin Overview");
    }

    private void navigate(String item) {
        if ("Logout".equals(item)) {
            if (JOptionPane.showConfirmDialog(this, "Sign out?", "Confirm", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
                onLogout.run();
            return;
        }
        nav.setActive(item);
        JPanel page = switch (item) {
            case "Customer Management" -> customers();
            case "Account Management" -> accounts();
            case "Transaction Management" -> transactions(null);
            case "Deposit Management" -> transactions("DEPOSIT");
            case "Withdrawal Management" -> transactions("WITHDRAWAL");
            case "Fund Transfers" -> transactions("TRANSFER");
            case "Fraud Alerts" -> fraud();
            case "Audit Logs" -> audit();
            case "Admin Profile", "Settings" -> profile();
            default -> overview();
        };
        content.add(page, item);
        cards.show(content, item);
    }

    private JPanel overview() {
        JPanel page = UiTheme.page();
        page.add(header("Administration", session.username()), BorderLayout.NORTH);
        JPanel metrics = new JPanel(new GridLayout(2, 4, 12, 12));
        metrics.setOpaque(false);
        try {
            DashboardStats s = bank.adminDashboard();
            metrics.add(new MetricCard("Customers", String.valueOf(s.customerCount()), UiTheme.PRIMARY));
            metrics.add(new MetricCard("Active accounts", String.valueOf(s.activeAccountCount()), UiTheme.SUCCESS));
            metrics.add(new MetricCard("Bank balance", CustomerHome.money(s.totalBalance()), UiTheme.PRIMARY));
            metrics.add(new MetricCard("Transactions", String.valueOf(s.transactionCount()), UiTheme.TEXT));
            metrics.add(new MetricCard("Monthly deposits", CustomerHome.money(s.monthlyDeposits()), UiTheme.SUCCESS));
            metrics.add(new MetricCard("Monthly withdrawals", CustomerHome.money(s.monthlyWithdrawals()), UiTheme.WARNING));
            metrics.add(new MetricCard("Open fraud alerts", String.valueOf(s.openFraudAlerts()),
                    s.openFraudAlerts() > 0 ? UiTheme.DANGER : UiTheme.SUCCESS));
            metrics.add(new MetricCard("All accounts", String.valueOf(s.accountCount()), UiTheme.MUTED));
        } catch (Exception ex) {
            metrics.add(new MetricCard("Data", "Unavailable", UiTheme.DANGER));
        }
        JPanel charts = new JPanel(new GridLayout(1, 3, 10, 0));
        charts.setOpaque(false);
        charts.setPreferredSize(new Dimension(0, 280));
        try {
            var series = bank.monthlySeries(null);
            var txs = bank.adminTransactionRows();
            charts.add(wrap(BankCharts.depositsVsWithdrawals(series)));
            charts.add(wrap(BankCharts.activityLine(series)));
            charts.add(wrap(BankCharts.typePie(txs)));
        } catch (Exception ex) {
            charts.add(UiTheme.muted(ex.getMessage()));
        }
        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.add(metrics, BorderLayout.NORTH);
        body.add(charts, BorderLayout.CENTER);
        try {
            body.add(wrap(BankCharts.growthLine(bank.customerGrowth())), BorderLayout.SOUTH);
        } catch (Exception ignored) { }
        page.add(body, BorderLayout.CENTER);
        JButton refresh = UiTheme.ghost("Refresh dashboard");
        refresh.addActionListener(e -> navigate("Admin Overview"));
        page.add(refresh, BorderLayout.SOUTH);
        return page;
    }

    private JPanel customers() {
        JPanel page = UiTheme.page();
        page.add(header("Customers", "Search by name or username"), BorderLayout.NORTH);
        try {
            DefaultTableModel model = CustomerHome.model(new String[]{"ID", "Username", "Name", "Email", "Phone", "Role"});
            for (var c : bank.allCustomers()) {
                model.addRow(new Object[]{c.customerId(), c.username(), c.fullName(), c.email(), c.phone(), c.role()});
            }
            page.add(new TablePanel(model), BorderLayout.CENTER);
        } catch (Exception ex) {
            page.add(UiTheme.muted(ex.getMessage()), BorderLayout.CENTER);
        }
        return page;
    }

    private JPanel accounts() {
        JPanel page = UiTheme.page();
        page.add(header("Accounts", "Open accounts and change status"), BorderLayout.NORTH);
        JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        tools.setOpaque(false);
        JTextField customerId = UiTheme.field();
        customerId.setColumns(8);
        JComboBox<String> type = new JComboBox<>(new String[]{"SAVINGS", "CURRENT"});
        JButton create = UiTheme.primary("Create account");
        JTextField accountNo = UiTheme.field();
        accountNo.setColumns(12);
        JComboBox<String> status = new JComboBox<>(new String[]{"ACTIVE", "FROZEN", "CLOSED"});
        JButton apply = UiTheme.ghost("Update status");
        tools.add(new JLabel("Customer ID"));
        tools.add(customerId);
        tools.add(type);
        tools.add(create);
        tools.add(new JLabel("Account"));
        tools.add(accountNo);
        tools.add(status);
        tools.add(apply);
        try {
            DefaultTableModel model = CustomerHome.model(new String[]{"Account", "Type", "Balance", "Status", "Customer ID"});
            List<AccountInfo> list = bank.allAccounts();
            for (AccountInfo a : list) {
                model.addRow(new Object[]{a.accountNumber(), a.accountType(), CustomerHome.money(a.balance()), a.status(), a.customerId()});
            }
            TablePanel table = new TablePanel(model);
            table.table().getSelectionModel().addListSelectionListener(e -> {
                int row = table.table().getSelectedRow();
                if (row >= 0) accountNo.setText(String.valueOf(table.table().getValueAt(row, 0)));
            });
            create.addActionListener(e -> {
                try {
                    String number = bank.createAccount(Long.parseLong(customerId.getText().trim()), String.valueOf(type.getSelectedItem()));
                    UiTheme.toast(this, "Created " + number, false);
                    navigate("Account Management");
                } catch (Exception ex) {
                    UiTheme.toast(this, ex.getMessage(), true);
                }
            });
            apply.addActionListener(e -> {
                try {
                    int ok = JOptionPane.showConfirmDialog(this, "Set " + accountNo.getText() + " to " + status.getSelectedItem() + "?",
                            "Confirm", JOptionPane.OK_CANCEL_OPTION);
                    if (ok != JOptionPane.OK_OPTION) return;
                    bank.setAccountStatus(session, accountNo.getText().trim(), String.valueOf(status.getSelectedItem()));
                    UiTheme.toast(this, "Status updated.", false);
                    navigate("Account Management");
                } catch (Exception ex) {
                    UiTheme.toast(this, ex.getMessage(), true);
                }
            });
            JPanel center = new JPanel(new BorderLayout(0, 8));
            center.setOpaque(false);
            center.add(tools, BorderLayout.NORTH);
            center.add(table, BorderLayout.CENTER);
            page.add(center, BorderLayout.CENTER);
        } catch (Exception ex) {
            page.add(UiTheme.muted(ex.getMessage()), BorderLayout.CENTER);
        }
        return page;
    }

    private JPanel transactions(String fixedType) {
        JPanel page = UiTheme.page();
        page.add(header(fixedType == null ? "All transactions" : fixedType + " activity", "Live ledger"), BorderLayout.NORTH);
        try {
            DefaultTableModel model = CustomerHome.txModel(bank.adminTransactionRows());
            TablePanel table = new TablePanel(model);
            if (fixedType != null) table.filterColumn(1, fixedType);
            JPanel ops = new JPanel(new FlowLayout(FlowLayout.LEFT));
            ops.setOpaque(false);
            JTextField acct = UiTheme.field();
            JTextField amt = UiTheme.field();
            JTextField dest = UiTheme.field();
            JButton run = UiTheme.primary(fixedType == null ? "Record transfer" : "Record " + fixedType.toLowerCase());
            ops.add(new JLabel("Account"));
            ops.add(acct);
            if ("TRANSFER".equals(fixedType) || fixedType == null) {
                ops.add(new JLabel("To"));
                ops.add(dest);
            }
            ops.add(new JLabel("Amount"));
            ops.add(amt);
            ops.add(run);
            run.addActionListener(e -> {
                try {
                    BigDecimal value = new BigDecimal(amt.getText().trim()).setScale(2, RoundingMode.HALF_UP);
                    String mode = fixedType == null ? "TRANSFER" : fixedType;
                    int ok = JOptionPane.showConfirmDialog(this, "Post " + mode + " of " + value + "?", "Confirm", JOptionPane.OK_CANCEL_OPTION);
                    if (ok != JOptionPane.OK_OPTION) return;
                    switch (mode) {
                        case "DEPOSIT" -> bank.depositAs(session, acct.getText().trim(), value);
                        case "WITHDRAWAL" -> bank.withdrawAs(session, acct.getText().trim(), value);
                        default -> bank.transferAs(session, acct.getText().trim(), dest.getText().trim(), value);
                    }
                    UiTheme.toast(this, "Posted.", false);
                    navigate(fixedType == null ? "Transaction Management" : switch (fixedType) {
                        case "DEPOSIT" -> "Deposit Management";
                        case "WITHDRAWAL" -> "Withdrawal Management";
                        default -> "Fund Transfers";
                    });
                } catch (Exception ex) {
                    UiTheme.toast(this, ex.getMessage(), true);
                }
            });
            JPanel center = new JPanel(new BorderLayout(0, 8));
            center.setOpaque(false);
            center.add(ops, BorderLayout.NORTH);
            center.add(table, BorderLayout.CENTER);
            page.add(center, BorderLayout.CENTER);
        } catch (Exception ex) {
            page.add(UiTheme.muted(ex.getMessage()), BorderLayout.CENTER);
        }
        return page;
    }

    private JPanel fraud() {
        JPanel page = UiTheme.page();
        page.add(header("Fraud alerts", "Large transactions waiting for review"), BorderLayout.NORTH);
        try {
            DefaultTableModel model = CustomerHome.model(new String[]{"ID", "Risk", "Reason", "Status", "Date", "Account", "Reference", "Amount"});
            for (Object[] r : bank.fraudAlerts()) model.addRow(r);
            TablePanel table = new TablePanel(model);
            JButton reviewed = UiTheme.ghost("Mark reviewed");
            JButton cleared = UiTheme.primary("Clear alert");
            reviewed.addActionListener(e -> updateAlert(table, "REVIEWED"));
            cleared.addActionListener(e -> updateAlert(table, "CLEARED"));
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
            actions.setOpaque(false);
            actions.add(reviewed);
            actions.add(cleared);
            JPanel center = new JPanel(new BorderLayout());
            center.setOpaque(false);
            center.add(table, BorderLayout.CENTER);
            center.add(actions, BorderLayout.SOUTH);
            page.add(center, BorderLayout.CENTER);
        } catch (Exception ex) {
            page.add(UiTheme.muted(ex.getMessage()), BorderLayout.CENTER);
        }
        return page;
    }

    private void updateAlert(TablePanel table, String status) {
        int view = table.table().getSelectedRow();
        if (view < 0) {
            UiTheme.toast(this, "Select an alert first.", true);
            return;
        }
        int row = table.table().convertRowIndexToModel(view);
        long id = ((Number) table.table().getModel().getValueAt(row, 0)).longValue();
        try {
            bank.setFraudAlertStatus(id, status);
            UiTheme.toast(this, "Alert " + id + " set to " + status + ".", false);
            navigate("Fraud Alerts");
        } catch (Exception ex) {
            UiTheme.toast(this, ex.getMessage(), true);
        }
    }

    private JPanel audit() {
        JPanel page = UiTheme.page();
        page.add(header("Audit logs", "Administrative and money-movement history"), BorderLayout.NORTH);
        try {
            DefaultTableModel model = CustomerHome.model(new String[]{"ID", "User", "Action", "Details", "When"});
            for (Object[] r : bank.auditLogs(300)) model.addRow(r);
            page.add(new TablePanel(model), BorderLayout.CENTER);
        } catch (Exception ex) {
            page.add(UiTheme.muted(ex.getMessage()), BorderLayout.CENTER);
        }
        return page;
    }

    private JPanel profile() {
        JPanel page = UiTheme.page();
        page.add(header("Admin profile", session.username() + " · " + session.role()), BorderLayout.NORTH);
        page.add(UiTheme.muted("Password hashes are never shown. Use Settings on a customer login to change a customer password, or update the hash only through the application."), BorderLayout.CENTER);
        return page;
    }

    private JPanel header(String title, String subtitle) {
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(UiTheme.heading(title), BorderLayout.NORTH);
        top.add(UiTheme.muted(LocalDate.now().format(DateTimeFormatter.ofPattern("d MMM yyyy")) + "  ·  " + subtitle), BorderLayout.SOUTH);
        return top;
    }

    private static JComponent wrap(JComponent c) {
        JPanel p = UiTheme.card();
        p.add(c, BorderLayout.CENTER);
        return p;
    }
}
