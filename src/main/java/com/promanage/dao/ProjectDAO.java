package com.promanage.dao;

import com.promanage.config.DatabaseConfig;
import com.promanage.model.Project;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProjectDAO {

    /**
     * Inserts a new project into the database.
     */
    public void addProject(Project project) throws SQLException {
        String sql = "INSERT INTO projects (project_code, title, deadline, revenue) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, project.getProjectCode());
            ps.setString(2, project.getTitle());
            ps.setInt(3, project.getDeadline());
            ps.setDouble(4, project.getRevenue());
            ps.executeUpdate();

            // Get the auto-generated project_id back and set it on the object
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                project.setProjectId(keys.getInt(1));
            }
        }
    }

    /**
     * Returns all projects ordered by most recently added first.
     */
    public List<Project> getAllProjects() throws SQLException {
        String sql = "SELECT * FROM projects ORDER BY created_at DESC";
        List<Project> list = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    // Converts one ResultSet row into a Project object
    private Project mapRow(ResultSet rs) throws SQLException {
        Project p = new Project();
        p.setProjectId(rs.getInt("project_id"));
        p.setProjectCode(rs.getString("project_code"));
        p.setTitle(rs.getString("title"));
        p.setDeadline(rs.getInt("deadline"));
        p.setRevenue(rs.getDouble("revenue"));
        p.setCreatedAt(rs.getTimestamp("created_at"));
        return p;
    }
}
