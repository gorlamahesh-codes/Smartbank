package com.smartbank.ui.components;

import com.smartbank.ui.theme.UiTheme;
import java.awt.*;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

public class TablePanel extends JPanel {
    private final JTable table;
    private final TableRowSorter<DefaultTableModel> sorter;

    public TablePanel(DefaultTableModel model) {
        super(new BorderLayout(0, 10));
        setOpaque(false);
        table = new JTable(model);
        table.setRowHeight(30);
        table.setFont(UiTheme.BODY);
        table.setShowGrid(false);
        table.setFillsViewportHeight(true);
        table.setSelectionBackground(new Color(0xE8F0FE));
        table.setSelectionForeground(UiTheme.TEXT);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        table.getTableHeader().setBackground(new Color(0xF2F4F7));
        table.setAutoCreateRowSorter(false);
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        JTextField search = UiTheme.field();
        search.putClientProperty("JTextField.placeholderText", "Search this table");
        search.setToolTipText("Filter rows");
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void apply() {
                String q = search.getText().trim();
                sorter.setRowFilter(q.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(q)));
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { apply(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { apply(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { apply(); }
        });

        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.setOpaque(false);
        bar.add(new JLabel("Search"), BorderLayout.WEST);
        bar.add(search, BorderLayout.CENTER);
        add(bar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    public JTable table() { return table; }

    public void filterColumn(int column, String valueOrAll) {
        if (valueOrAll == null || valueOrAll.equals("ALL") || valueOrAll.isBlank()) {
            sorter.setRowFilter(null);
            return;
        }
        sorter.setRowFilter(RowFilter.regexFilter("^" + Pattern.quote(valueOrAll) + "$", column));
    }
}
