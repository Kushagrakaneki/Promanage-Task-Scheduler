package com.promanage.dao;

import com.promanage.config.DatabaseConfig;
import com.promanage.model.Project;
import com.promanage.model.ScheduledProject;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ScheduleDAO {

    /**
     * Saves a weekly schedule to the database.
     * If a schedule already exists for that week, it is replaced.
     */
    public void saveSchedule(String weekLabel, List<ScheduledProject> schedule) throws SQLException {
        String deleteSql = "DELETE FROM schedules WHERE week_label = ?";
        String insertSql = "INSERT INTO schedules (week_label, project_id, assigned_day, day_name) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false); // Start transaction

            try {
                // Delete old entries for this week
                try (PreparedStatement del = conn.prepareStatement(deleteSql)) {
                    del.setString(1, weekLabel);
                    del.executeUpdate();
                }

                // Insert new entries
                try (PreparedStatement ins = conn.prepareStatement(insertSql)) {
                    for (ScheduledProject sp : schedule) {
                        ins.setString(1, weekLabel);
                        ins.setInt(2, sp.getProject().getProjectId());
                        ins.setInt(3, sp.getAssignedDay());
                        ins.setString(4, sp.getDayName());
                        ins.addBatch();
                    }
                    ins.executeBatch();
                }

                conn.commit();

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Retrieves all saved schedules with project details, ordered by week and day.
     */
    public List<ScheduledProject> getAllSchedules() throws SQLException {
        String sql = """
                SELECT s.week_label, s.assigned_day, s.day_name,
                       p.project_id, p.project_code, p.title, p.deadline, p.revenue, p.created_at
                FROM schedules s
                JOIN projects p ON s.project_id = p.project_id
                ORDER BY s.week_label, s.assigned_day
                """;

        return fetchScheduledProjects(sql);
    }

    /**
     * Retrieves the schedule for a specific week label.
     */
    public List<ScheduledProject> getScheduleByWeek(String weekLabel) throws SQLException {
        String sql = """
                SELECT s.week_label, s.assigned_day, s.day_name,
                       p.project_id, p.project_code, p.title, p.deadline, p.revenue, p.created_at
                FROM schedules s
                JOIN projects p ON s.project_id = p.project_id
                WHERE s.week_label = ?
                ORDER BY s.assigned_day
                """;

        List<ScheduledProject> list = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, weekLabel);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * Returns all distinct week labels saved in the database.
     */
    public List<String> getAllWeekLabels() throws SQLException {
        String sql    = "SELECT DISTINCT week_label FROM schedules ORDER BY week_label";
        List<String> labels = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {

            while (rs.next()) {
                labels.add(rs.getString("week_label"));
            }
        }
        return labels;
    }

    // Helper: run a query and map results to ScheduledProject list
    private List<ScheduledProject> fetchScheduledProjects(String sql) throws SQLException {
        List<ScheduledProject> list = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    // Converts one ResultSet row into a ScheduledProject object
    private ScheduledProject mapRow(ResultSet rs) throws SQLException {
        Project p = new Project();
        p.setProjectId(rs.getInt("project_id"));
        p.setProjectCode(rs.getString("project_code"));
        p.setTitle(rs.getString("title"));
        p.setDeadline(rs.getInt("deadline"));
        p.setRevenue(rs.getDouble("revenue"));
        p.setCreatedAt(rs.getTimestamp("created_at"));

        return new ScheduledProject(p, rs.getInt("assigned_day"), rs.getString("day_name"));
    }
}
