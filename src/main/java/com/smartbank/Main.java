package com.smartbank;

import com.formdev.flatlaf.FlatLightLaf;
import com.smartbank.ui.SmartBankFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new FlatLightLaf());
                // Subtle modern FlatLaf tweaks
                UIManager.put("Button.arc", 10);
                UIManager.put("Component.arc", 10);
                UIManager.put("TextComponent.arc", 8);
                UIManager.put("ScrollBar.width", 12);
            } catch (Exception ignored) { }
            new SmartBankFrame().setVisible(true);
        });
    }
}
