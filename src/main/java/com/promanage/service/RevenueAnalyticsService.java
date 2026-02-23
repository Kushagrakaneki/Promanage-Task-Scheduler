package com.promanage.service;

import com.promanage.dao.ScheduleDAO;
import com.promanage.model.MonthlyRevenueSummary;
import com.promanage.model.ScheduledProject;

import java.sql.SQLException;
import java.util.*;

public class RevenueAnalyticsService {

    private final ScheduleDAO scheduleDAO = new ScheduleDAO();

    // Full month names for display
    private static final String[] MONTH_NAMES = {
        "", "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    };

    /**
     * FEATURE 1 — Monthly Revenue Summary
     *
     * Reads all saved weekly schedules from the DB.
     * Groups them by month (extracted from week_label like "Week-2024-03").
     * Calculates: total revenue, number of projects, number of weeks for each month.
     */
    public List<MonthlyRevenueSummary> getMonthlyRevenueSummary() throws SQLException {

        // Get all week labels saved in the database
        List<String> weekLabels = scheduleDAO.getAllWeekLabels();

        if (weekLabels.isEmpty()) {
            return new ArrayList<>();
        }

        // Map: "YYYY-MM" → [totalRevenue, projectCount, weekCount]
        // Using LinkedHashMap to keep month order
        Map<String, double[]> monthData = new LinkedHashMap<>();

        for (String weekLabel : weekLabels) {
            // weekLabel format: "Week-2024-03"  (Year-WeekNumber)
            // We'll convert ISO week number to approximate month
            String yearMonth = weekLabelToYearMonth(weekLabel);
            if (yearMonth == null) continue;

            // Get all projects scheduled in this week
            List<ScheduledProject> scheduled = scheduleDAO.getScheduleByWeek(weekLabel);

            double weekRevenue = scheduled.stream()
                    .mapToDouble(sp -> sp.getProject().getRevenue())
                    .sum();

            // Initialize if this month hasn't been seen yet
            monthData.putIfAbsent(yearMonth, new double[]{0, 0, 0});

            double[] data = monthData.get(yearMonth);
            data[0] += weekRevenue;          // total revenue
            data[1] += scheduled.size();     // total projects
            data[2] += 1;                    // weeks counted
        }

        // Convert the map to a list of MonthlyRevenueSummary objects
        List<MonthlyRevenueSummary> summaries = new ArrayList<>();

        for (Map.Entry<String, double[]> entry : monthData.entrySet()) {
            String[] parts = entry.getKey().split("-");
            int    year    = Integer.parseInt(parts[0]);
            int    month   = Integer.parseInt(parts[1]);
            double[] data  = entry.getValue();

            summaries.add(new MonthlyRevenueSummary(
                    year,
                    month,
                    MONTH_NAMES[month],
                    data[0],
                    (int) data[1],
                    (int) data[2]
            ));
        }

        return summaries;
    }

    /**
     * FEATURE 2 — Predicted Revenue for Next Month
     *
     * Takes the average of all past months' revenue and uses that
     * as a simple prediction for next month.
     *
     * Formula: Predicted = Sum of all monthly revenues / Number of months
     *
     * If fewer than 2 months of data exist, we note the prediction may not be reliable.
     */
    public double predictNextMonthRevenue(List<MonthlyRevenueSummary> summaries) {
        if (summaries.isEmpty()) return 0;

        double total = summaries.stream()
                .mapToDouble(MonthlyRevenueSummary::getTotalRevenue)
                .sum();

        return total / summaries.size(); // simple moving average
    }

    /**
     * Returns how confident the prediction is based on how many months of data we have.
     */
    public String getPredictionConfidence(int monthCount) {
        if (monthCount >= 6) return "High (6+ months of data)";
        if (monthCount >= 3) return "Medium (3-5 months of data)";
        if (monthCount >= 2) return "Low (2 months of data)";
        return "Very Low (only 1 month — need more data for reliable prediction)";
    }

    /**
     * Converts a week label like "Week-2024-03" to a "YYYY-MM" string.
     *
     * ISO Week 1-4   → Month 1  (January)
     * ISO Week 5-8   → Month 2  (February)
     * ISO Week 9-13  → Month 3  (March)
     * ... etc.
     *
     * This is an approximation. Accurate enough for a scheduling system.
     */
    private String weekLabelToYearMonth(String weekLabel) {
        try {
            // Format: "Week-2024-03"
            String[] parts   = weekLabel.split("-");
            int      year    = Integer.parseInt(parts[1]);
            int      weekNum = Integer.parseInt(parts[2]);

            // Approximate: every 4-5 weeks = 1 month
            int month = (int) Math.ceil(weekNum / 4.333);
            if (month < 1)  month = 1;
            if (month > 12) month = 12;

            return String.format("%d-%02d", year, month);

        } catch (Exception e) {
            return null; // skip malformed labels
        }
    }
}
