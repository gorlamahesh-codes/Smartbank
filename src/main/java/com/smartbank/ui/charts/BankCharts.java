package com.smartbank.ui.charts;

import com.smartbank.ui.theme.UiTheme;
import java.awt.Color;
import java.math.BigDecimal;
import java.util.List;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

public final class BankCharts {
    private BankCharts() {}

    /** series rows: [month, deposits, withdrawals] from MySQL. */
    public static ChartPanel depositsVsWithdrawals(List<Object[]> series) {
        DefaultCategoryDataset data = new DefaultCategoryDataset();
        if (series != null) {
            for (Object[] row : series) {
                String month = String.valueOf(row[0]);
                data.addValue(number(row[1]), "Deposits", month);
                data.addValue(number(row[2]), "Withdrawals", month);
            }
        }
        JFreeChart chart = ChartFactory.createBarChart(
                "Deposits vs withdrawals", "Month", "Amount (INR)",
                data, PlotOrientation.VERTICAL, true, true, false);
        style(chart);
        return wrap(chart);
    }

    public static ChartPanel activityLine(List<Object[]> series) {
        DefaultCategoryDataset data = new DefaultCategoryDataset();
        if (series != null) {
            for (Object[] row : series) {
                String month = String.valueOf(row[0]);
                BigDecimal dep = decimal(row[1]);
                BigDecimal wit = decimal(row[2]);
                data.addValue(dep.subtract(wit), "Net flow", month);
                data.addValue(dep, "Deposits", month);
            }
        }
        JFreeChart chart = ChartFactory.createLineChart(
                "Monthly activity (from ledger)", "Month", "INR",
                data, PlotOrientation.VERTICAL, true, true, false);
        style(chart);
        return wrap(chart);
    }

    /** Transaction rows: index 1 is type. */
    public static ChartPanel typePie(List<Object[]> rows) {
        DefaultPieDataset<String> data = new DefaultPieDataset<>();
        int dep = 0, wit = 0, tr = 0;
        if (rows != null) {
            for (Object[] row : rows) {
                String type = String.valueOf(row[1]);
                if ("DEPOSIT".equals(type)) dep++;
                else if ("WITHDRAWAL".equals(type)) wit++;
                else if ("TRANSFER".equals(type)) tr++;
            }
        }
        if (dep + wit + tr == 0) data.setValue("No transactions", 1);
        else {
            if (dep > 0) data.setValue("Deposit", dep);
            if (wit > 0) data.setValue("Withdrawal", wit);
            if (tr > 0) data.setValue("Transfer", tr);
        }
        JFreeChart chart = ChartFactory.createPieChart("Transaction mix", data, true, true, false);
        PiePlot<?> plot = (PiePlot<?>) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        chart.setBackgroundPaint(Color.WHITE);
        return wrap(chart);
    }

    public static ChartPanel growthLine(List<Object[]> series) {
        DefaultCategoryDataset data = new DefaultCategoryDataset();
        if (series != null) {
            for (Object[] row : series) data.addValue(number(row[1]), "New customers", String.valueOf(row[0]));
        }
        JFreeChart chart = ChartFactory.createLineChart(
                "Customer registrations", "Month", "Customers",
                data, PlotOrientation.VERTICAL, false, true, false);
        style(chart);
        return wrap(chart);
    }

    private static ChartPanel wrap(JFreeChart chart) {
        ChartPanel panel = new ChartPanel(chart);
        panel.setOpaque(false);
        panel.setPreferredSize(new java.awt.Dimension(420, 260));
        panel.setMouseWheelEnabled(false);
        return panel;
    }

    private static void style(JFreeChart chart) {
        chart.setBackgroundPaint(Color.WHITE);
        if (chart.getPlot() instanceof CategoryPlot plot) {
            plot.setBackgroundPaint(new Color(0xF8FAFC));
            plot.setRangeGridlinePaint(UiTheme.LINE);
            plot.setOutlineVisible(false);
        }
    }

    private static Number number(Object o) {
        if (o instanceof Number n) return n;
        return 0;
    }

    private static BigDecimal decimal(Object o) {
        if (o instanceof BigDecimal b) return b;
        if (o instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return BigDecimal.ZERO;
    }
}
