package com.promanage.util;

import com.promanage.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class CodeGenerator {

    /**
     * Looks at the highest existing project_code in the DB and returns the next one.
     * E.g. if "PRJ007" exists, returns "PRJ008".
     * If no projects exist, returns "PRJ001".
     */
    public static String generateProjectCode() {
        String sql = "SELECT project_code FROM projects ORDER BY project_id DESC LIMIT 1";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {

            if (rs.next()) {
                String lastCode = rs.getString("project_code"); // e.g. "PRJ007"
                int number = Integer.parseInt(lastCode.replace("PRJ", ""));
                return String.format("PRJ%03d", number + 1);
            } else {
                return "PRJ001";
            }

        } catch (SQLException e) {
            System.out.println("Warning: Could not generate project code. Using default.");
            return "PRJ001";
        }
    }
}
