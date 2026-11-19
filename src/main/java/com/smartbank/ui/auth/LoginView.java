package com.smartbank.ui.auth;

import com.smartbank.ui.theme.UiTheme;
import java.awt.*;
import java.util.function.BiConsumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class LoginView extends JPanel {
    public LoginView(String rememberedUser, BiConsumer<String, String> onLogin, Runnable onRegister, java.util.function.Consumer<String> onForgot, java.util.function.Consumer<Boolean> remember) {
        super(new GridLayout(1, 2));
        setBackground(UiTheme.BG);

        JPanel brand = new JPanel(new GridBagLayout());
        brand.setBackground(UiTheme.SIDEBAR);
        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JLabel logo = new JLabel("SmartBank");
        logo.setForeground(Color.WHITE);
        logo.setFont(new Font("SansSerif", Font.BOLD, 40));
        JLabel tag = new JLabel("<html><body style='width:280px;color:#D0D5DD'>Everyday banking with a clear view of your money, accounts and transfers.</body></html>");
        tag.setBorder(new EmptyBorder(12, 0, 0, 0));
        copy.add(logo);
        copy.add(tag);
        brand.add(copy);

        JPanel formWrap = new JPanel(new GridBagLayout());
        formWrap.setBackground(UiTheme.BG);
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(UiTheme.CARD);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.LINE),
                new EmptyBorder(36, 36, 36, 36)));
        form.setPreferredSize(new Dimension(420, 520));

        JLabel welcome = new JLabel("Welcome back");
        welcome.setFont(UiTheme.TITLE);
        welcome.setForeground(UiTheme.TEXT);
        welcome.setAlignmentX(LEFT_ALIGNMENT);
        JLabel sub = UiTheme.muted("Sign in to your SmartBank account");
        sub.setAlignmentX(LEFT_ALIGNMENT);
        sub.setBorder(new EmptyBorder(4, 0, 18, 0));

        JTextField user = UiTheme.field();
        user.setText(rememberedUser == null ? "" : rememberedUser);
        user.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        user.setAlignmentX(LEFT_ALIGNMENT);
        JPasswordField pass = UiTheme.password();
        pass.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        pass.setAlignmentX(LEFT_ALIGNMENT);
        JCheckBox show = new JCheckBox("Show password");
        show.setOpaque(false);
        show.setAlignmentX(LEFT_ALIGNMENT);
        show.addActionListener(e -> pass.setEchoChar(show.isSelected() ? (char) 0 : '\u2022'));
        JCheckBox rememberBox = new JCheckBox("Remember username", rememberedUser != null && !rememberedUser.isBlank());
        rememberBox.setOpaque(false);
        rememberBox.setAlignmentX(LEFT_ALIGNMENT);

        JButton signIn = UiTheme.primary("Sign in");
        signIn.setAlignmentX(LEFT_ALIGNMENT);
        signIn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        JButton create = UiTheme.ghost("Create account");
        create.setAlignmentX(LEFT_ALIGNMENT);
        create.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        JButton forgot = new JButton("Forgot password?");
        forgot.setBorderPainted(false);
        forgot.setContentAreaFilled(false);
        forgot.setForeground(UiTheme.PRIMARY);
        forgot.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        forgot.setAlignmentX(LEFT_ALIGNMENT);

        form.add(welcome);
        form.add(sub);
        form.add(label("Username"));
        form.add(user);
        form.add(Box.createVerticalStrut(10));
        form.add(label("Password"));
        form.add(pass);
        form.add(show);
        form.add(rememberBox);
        form.add(Box.createVerticalStrut(16));
        form.add(signIn);
        form.add(Box.createVerticalStrut(8));
        form.add(create);
        form.add(forgot);
        formWrap.add(form);

        add(brand);
        add(formWrap);

        Runnable submit = () -> {
            remember.accept(rememberBox.isSelected());
            onLogin.accept(user.getText().trim(), new String(pass.getPassword()));
        };
        signIn.addActionListener(e -> submit.run());
        pass.addActionListener(e -> submit.run());
        create.addActionListener(e -> onRegister.run());
        forgot.addActionListener(e -> onForgot.accept(user.getText().trim()));
    }

    private static JLabel label(String t) {
        JLabel l = new JLabel(t);
        l.setForeground(UiTheme.MUTED);
        l.setAlignmentX(LEFT_ALIGNMENT);
        l.setBorder(new EmptyBorder(0, 0, 4, 0));
        return l;
    }
}
