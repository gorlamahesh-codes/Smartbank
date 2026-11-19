package com.smartbank.ui.auth;

import com.smartbank.ui.theme.UiTheme;
import java.awt.*;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class RegisterView extends JPanel {
    public record Registration(String username, String password, String fullName, String email, String phone, String address) {}

    public RegisterView(Consumer<Registration> onSubmit, Runnable onBack) {
        super(new BorderLayout());
        setBackground(UiTheme.BG);
        setBorder(new EmptyBorder(28, 40, 28, 40));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(UiTheme.heading("Create your account"), BorderLayout.NORTH);
        head.add(UiTheme.muted("Register as a customer. An administrator will open your first account."), BorderLayout.SOUTH);
        add(head, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(0, 2, 14, 12));
        form.setOpaque(false);
        form.setBorder(new EmptyBorder(20, 0, 12, 0));
        JTextField full = UiTheme.field();
        JTextField user = UiTheme.field();
        JTextField email = UiTheme.field();
        JTextField phone = UiTheme.field();
        JTextField address = UiTheme.field();
        JPasswordField pass = UiTheme.password();
        JPasswordField confirm = UiTheme.password();
        addRow(form, "Full name", full);
        addRow(form, "Username", user);
        addRow(form, "Email", email);
        addRow(form, "Phone", phone);
        addRow(form, "Address", address);
        addRow(form, "Password", pass);
        addRow(form, "Confirm password", confirm);
        add(form, BorderLayout.CENTER);

        JButton create = UiTheme.primary("Create account");
        JButton back = UiTheme.ghost("Back to sign in");
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.setOpaque(false);
        actions.add(create);
        actions.add(back);
        add(actions, BorderLayout.SOUTH);

        create.addActionListener(e -> {
            String p1 = new String(pass.getPassword());
            String p2 = new String(confirm.getPassword());
            if (full.getText().isBlank() || user.getText().isBlank() || email.getText().isBlank()
                    || phone.getText().isBlank() || address.getText().isBlank()) {
                UiTheme.toast(this, "All fields are required.", true);
                return;
            }
            if (!email.getText().contains("@") || !email.getText().contains(".")) {
                UiTheme.toast(this, "Enter a valid email address.", true);
                return;
            }
            if (!phone.getText().trim().matches("[0-9+\\- ]{8,20}")) {
                UiTheme.toast(this, "Enter a valid phone number.", true);
                return;
            }
            if (p1.length() < 8) {
                UiTheme.toast(this, "Password must contain at least 8 characters.", true);
                return;
            }
            if (!p1.equals(p2)) {
                UiTheme.toast(this, "Passwords do not match.", true);
                return;
            }
            onSubmit.accept(new Registration(user.getText().trim(), p1, full.getText().trim(),
                    email.getText().trim(), phone.getText().trim(), address.getText().trim()));
        });
        back.addActionListener(e -> onBack.run());
    }

    private static void addRow(JPanel form, String label, JComponent field) {
        JPanel cell = new JPanel(new BorderLayout(0, 4));
        cell.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setForeground(UiTheme.MUTED);
        cell.add(l, BorderLayout.NORTH);
        cell.add(field, BorderLayout.CENTER);
        form.add(cell);
    }
}
