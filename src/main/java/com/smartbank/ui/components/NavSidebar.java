package com.smartbank.ui.components;

import com.smartbank.ui.theme.UiTheme;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class NavSidebar extends JPanel {
    private final Map<String, JButton> buttons = new LinkedHashMap<>();
    private String active = "";

    public NavSidebar(String roleLabel, String[] items, Consumer<String> onSelect) {
        setBackground(UiTheme.SIDEBAR);
        setPreferredSize(new Dimension(240, 0));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(8, 0, 12, 0));

        JLabel brand = new JLabel("  SmartBank");
        brand.setForeground(Color.WHITE);
        brand.setFont(new Font("SansSerif", Font.BOLD, 22));
        brand.setBorder(new EmptyBorder(22, 14, 2, 10));
        brand.setAlignmentX(LEFT_ALIGNMENT);
        add(brand);

        JLabel role = new JLabel("  " + roleLabel);
        role.setForeground(new Color(0x98A2B3));
        role.setFont(new Font("SansSerif", Font.PLAIN, 12));
        role.setBorder(new EmptyBorder(0, 14, 22, 10));
        role.setAlignmentX(LEFT_ALIGNMENT);
        add(role);

        for (String item : items) {
            JButton b = new JButton("   " + item);
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            b.setAlignmentX(LEFT_ALIGNMENT);
            b.setHorizontalAlignment(SwingConstants.LEFT);
            b.setForeground(Color.WHITE);
            b.setBackground(UiTheme.SIDEBAR);
            b.setFont(new Font("SansSerif", Font.PLAIN, 14));
            b.setFocusPainted(false);
            b.setBorder(new EmptyBorder(8, 12, 8, 8));
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.addActionListener(e -> onSelect.accept(item));
            b.addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) {
                    if (!item.equals(active)) b.setBackground(UiTheme.SIDEBAR_HOVER);
                }
                @Override public void mouseExited(MouseEvent e) {
                    if (!item.equals(active)) b.setBackground(UiTheme.SIDEBAR);
                }
            });
            buttons.put(item, b);
            add(b);
        }
    }

    public void setActive(String name) {
        active = name;
        for (var e : buttons.entrySet()) {
            boolean on = e.getKey().equals(name);
            e.getValue().setBackground(on ? UiTheme.SIDEBAR_ACTIVE : UiTheme.SIDEBAR);
            e.getValue().setFont(new Font("SansSerif", on ? Font.BOLD : Font.PLAIN, 14));
        }
    }
}
