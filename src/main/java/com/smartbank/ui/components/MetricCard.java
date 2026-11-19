package com.smartbank.ui.components;

import com.smartbank.ui.theme.UiTheme;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class MetricCard extends JPanel {
    public MetricCard(String label, String value, Color accent) {
        setLayout(new BorderLayout(4, 8));
        setBackground(UiTheme.CARD);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(UiTheme.LINE),
                        new EmptyBorder(16, 16, 16, 16))));
        JLabel name = new JLabel(label);
        name.setForeground(UiTheme.MUTED);
        name.setFont(new Font("SansSerif", Font.PLAIN, 13));
        JLabel amount = new JLabel(value);
        amount.setForeground(UiTheme.TEXT);
        amount.setFont(new Font("SansSerif", Font.BOLD, 22));
        add(name, BorderLayout.NORTH);
        add(amount, BorderLayout.CENTER);
        setToolTipText(label + ": " + value);
    }
}
