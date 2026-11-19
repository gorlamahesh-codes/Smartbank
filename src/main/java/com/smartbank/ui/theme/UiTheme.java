package com.smartbank.ui.theme;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public final class UiTheme {
    public static final Color PRIMARY = new Color(0x2457A7);
    public static final Color PRIMARY_HOVER = new Color(0x1E4A90);
    public static final Color SIDEBAR = new Color(0x14243B);
    public static final Color SIDEBAR_HOVER = new Color(0x1C3254);
    public static final Color SIDEBAR_ACTIVE = new Color(0x2457A7);
    public static final Color BG = new Color(0xF5F7FB);
    public static final Color CARD = Color.WHITE;
    public static final Color TEXT = new Color(0x1D2939);
    public static final Color MUTED = new Color(0x667085);
    public static final Color SUCCESS = new Color(0x16A36A);
    public static final Color WARNING = new Color(0xE9A23B);
    public static final Color DANGER = new Color(0xD94B4B);
    public static final Color LINE = new Color(0xE4E7EC);
    public static final Font TITLE = new Font("SansSerif", Font.BOLD, 26);
    public static final Font H2 = new Font("SansSerif", Font.BOLD, 16);
    public static final Font BODY = new Font("SansSerif", Font.PLAIN, 14);

    private UiTheme() {}

    public static JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(TITLE);
        l.setForeground(TEXT);
        return l;
    }

    public static JLabel muted(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(MUTED);
        l.setFont(BODY);
        return l;
    }

    public static JButton primary(String text) {
        JButton b = new JButton(text);
        b.setBackground(PRIMARY);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.BOLD, 14));
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(10, 18, 10, 18));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static JButton ghost(String text) {
        JButton b = new JButton(text);
        b.setBackground(new Color(0xEEF2F7));
        b.setForeground(TEXT);
        b.setFont(BODY);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(10, 16, 10, 16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static JTextField field() {
        JTextField f = new JTextField();
        style(f);
        return f;
    }

    public static JPasswordField password() {
        JPasswordField f = new JPasswordField();
        style(f);
        return f;
    }

    public static void style(JTextField f) {
        f.setFont(BODY);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE),
                new EmptyBorder(10, 12, 10, 12)));
    }

    public static JPanel card() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBackground(CARD);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE),
                new EmptyBorder(16, 18, 16, 18)));
        return p;
    }

    public static JPanel page() {
        JPanel p = new JPanel(new BorderLayout(0, 16));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(8, 4, 8, 4));
        return p;
    }

    public static void toast(Component parent, String message, boolean error) {
        JOptionPane.showMessageDialog(parent, message, "SmartBank",
                error ? JOptionPane.ERROR_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
    }
}
