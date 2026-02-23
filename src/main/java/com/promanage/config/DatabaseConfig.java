package com.promanage.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConfig {

    // ============================================================
    // CHANGE THESE IF YOUR POSTGRESQL CREDENTIALS ARE DIFFERENT
    // ============================================================
    private static final String HOST     = "localhost";
    private static final String PORT     = "5432";
    private static final String DATABASE = "promanage_db";
    private static final String USERNAME = "postgres";
    private static final String PASSWORD = "kushagrakaneki";
    // ============================================================

    private static final String URL = "jdbc:postgresql://" + HOST + ":" + PORT + "/" + DATABASE;

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }

    /**
     * Called once at startup to create all tables if they don't exist yet.
     */
    public static void initializeDatabase() {
        String createProjects = """
                CREATE TABLE IF NOT EXISTS projects (
                    project_id   SERIAL PRIMARY KEY,
                    project_code VARCHAR(10) UNIQUE NOT NULL,
                    title        VARCHAR(255) NOT NULL,
                    deadline     INT NOT NULL CHECK (deadline BETWEEN 1 AND 5),
                    revenue      DECIMAL(12,2) NOT NULL CHECK (revenue > 0),
                    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;

        String createSchedules = """
                CREATE TABLE IF NOT EXISTS schedules (
                    schedule_id  SERIAL PRIMARY KEY,
                    week_label   VARCHAR(20) NOT NULL,
                    project_id   INT REFERENCES projects(project_id),
                    assigned_day INT NOT NULL CHECK (assigned_day BETWEEN 1 AND 5),
                    day_name     VARCHAR(10) NOT NULL,
                    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createProjects);
            stmt.execute(createSchedules);
            System.out.println("Database ready.");

        } catch (SQLException e) {
            System.out.println("ERROR: Could not initialize database.");
            System.out.println("Details: " + e.getMessage());
            System.out.println("\nPlease make sure:");
            System.out.println("  1. PostgreSQL is running");
            System.out.println("  2. Database 'promanage_db' exists");
            System.out.println("  3. Username/password in DatabaseConfig.java are correct");
            System.exit(1);
        }
    }
}
