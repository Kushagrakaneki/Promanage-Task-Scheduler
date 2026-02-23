package com.promanage;

import com.promanage.config.DatabaseConfig;
import com.promanage.dao.ProjectDAO;
import com.promanage.dao.ScheduleDAO;
import com.promanage.model.MonthlyRevenueSummary;
import com.promanage.model.Project;
import com.promanage.model.ScheduledProject;
import com.promanage.service.RevenueAnalyticsService;
import com.promanage.service.SchedulerService;
import com.promanage.util.CodeGenerator;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class Main {

    // These are shared across all menu options
    private static final ProjectDAO            projectDAO      = new ProjectDAO();
    private static final ScheduleDAO           scheduleDAO     = new ScheduleDAO();
    private static final SchedulerService      schedulerSvc    = new SchedulerService();
    private static final RevenueAnalyticsService analyticsSvc  = new RevenueAnalyticsService();
    private static final Scanner               scanner         = new Scanner(System.in);

    public static void main(String[] args) {

        // Step 1: Connect to DB and create tables if they don't exist
        DatabaseConfig.initializeDatabase();

        // Step 2: Show the menu in a loop until user chooses Exit
        boolean running = true;
        while (running) {
            printMenu();
            int choice = readInt("Enter choice: ", 1, 7);

            switch (choice) {
                case 1 -> addProject();
                case 2 -> viewAllProjects();
                case 3 -> generateSchedule();
                case 4 -> viewSavedSchedule();
                case 5 -> viewMonthlyRevenueSummary();
                case 6 -> viewRevenuePrediction();
                case 7 -> {
                    System.out.println("\nGoodbye! Thank you for using ProManage Scheduler.");
                    running = false;
                }
            }
        }

        scanner.close();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  MENU DISPLAY
    // ─────────────────────────────────────────────────────────────────────────

    private static void printMenu() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║   ProManage Solutions — Project Scheduler ║");
        System.out.println("╠══════════════════════════════════════════╣");
        System.out.println("║  1. Add New Project                       ║");
        System.out.println("║  2. View All Projects                     ║");
        System.out.println("║  3. Generate Optimal Weekly Schedule      ║");
        System.out.println("║  4. View Saved Schedule                   ║");
        System.out.println("║  5. Monthly Revenue Summary               ║");
        System.out.println("║  6. Predict Next Month Revenue            ║");
        System.out.println("║  7. Exit                                  ║");
        System.out.println("╚══════════════════════════════════════════╝");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  OPTION 1: ADD PROJECT
    // ─────────────────────────────────────────────────────────────────────────

    private static void addProject() {
        System.out.println("\n--- Add New Project ---");

        System.out.print("Enter project title: ");
        String title = scanner.nextLine().trim();

        if (title.isEmpty()) {
            System.out.println("Title cannot be empty. Returning to menu.");
            return;
        }

        int    deadline = readInt("Enter deadline (1-5 working days): ", 1, 5);
        double revenue  = readDouble("Enter expected revenue (INR): ");

        String  code    = CodeGenerator.generateProjectCode();
        Project project = new Project(code, title, deadline, revenue);

        try {
            projectDAO.addProject(project);
            System.out.println("\n✓ Project added successfully!");
            System.out.println("  Code    : " + project.getProjectCode());
            System.out.println("  Title   : " + project.getTitle());
            System.out.println("  Deadline: Day " + project.getDeadline());
            System.out.println("  Revenue : INR " + formatMoney(project.getRevenue()));
        } catch (SQLException e) {
            System.out.println("ERROR: Could not save project. " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  OPTION 2: VIEW ALL PROJECTS
    // ─────────────────────────────────────────────────────────────────────────

    private static void viewAllProjects() {
        System.out.println("\n--- All Projects ---");

        try {
            List<Project> projects = projectDAO.getAllProjects();

            if (projects.isEmpty()) {
                System.out.println("No projects found. Add some projects first.");
                return;
            }

            // Table header
            System.out.println();
            System.out.printf("%-10s %-30s %-10s %-18s %-20s%n",
                    "Code", "Title", "Deadline", "Revenue (INR)", "Added On");
            System.out.println("-".repeat(92));

            for (Project p : projects) {
                System.out.printf("%-10s %-30s %-10s %-18s %-20s%n",
                        p.getProjectCode(),
                        truncate(p.getTitle(), 28),
                        "Day " + p.getDeadline(),
                        formatMoney(p.getRevenue()),
                        p.getCreatedAt().toString().substring(0, 16)
                );
            }

            System.out.println("-".repeat(92));
            System.out.println("Total projects: " + projects.size());

        } catch (SQLException e) {
            System.out.println("ERROR: Could not load projects. " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  OPTION 3: GENERATE OPTIMAL SCHEDULE
    // ─────────────────────────────────────────────────────────────────────────

    private static void generateSchedule() {
        System.out.println("\n--- Generate Optimal Weekly Schedule ---");

        try {
            List<Project> allProjects = projectDAO.getAllProjects();

            if (allProjects.isEmpty()) {
                System.out.println("No projects available. Please add projects first.");
                return;
            }

            // Run the greedy scheduling algorithm
            List<ScheduledProject> schedule = schedulerSvc.generateOptimalSchedule(allProjects);

            if (schedule.isEmpty()) {
                System.out.println("Could not schedule any projects. Check deadlines.");
                return;
            }

            // Calculate totals
            double totalRevenue = schedule.stream()
                    .mapToDouble(sp -> sp.getProject().getRevenue())
                    .sum();

            int unscheduled = allProjects.size() - schedule.size();

            // Display the schedule
            System.out.println();
            System.out.println("╔══════════════════════════════════════════════════════════════════╗");
            System.out.println("║                  OPTIMAL WEEKLY SCHEDULE                        ║");
            System.out.println("╚══════════════════════════════════════════════════════════════════╝");
            System.out.println();
            System.out.printf("%-5s %-12s %-10s %-30s %-15s%n",
                    "Day", "Day Name", "Code", "Title", "Revenue (INR)");
            System.out.println("-".repeat(76));

            for (ScheduledProject sp : schedule) {
                Project p = sp.getProject();
                System.out.printf("%-5d %-12s %-10s %-30s %-15s%n",
                        sp.getAssignedDay(),
                        sp.getDayName(),
                        p.getProjectCode(),
                        truncate(p.getTitle(), 28),
                        formatMoney(p.getRevenue())
                );
            }

            System.out.println("-".repeat(76));
            System.out.printf("%-28s INR %s%n", "Total Revenue:", formatMoney(totalRevenue));
            System.out.printf("Projects Scheduled : %d out of %d%n", schedule.size(), allProjects.size());
            if (unscheduled > 0) {
                System.out.printf("Projects NOT scheduled (missed deadline or no slot): %d%n", unscheduled);
            }

            // Ask if they want to save
            System.out.print("\nSave this schedule to database? (yes/no): ");
            String answer = scanner.nextLine().trim().toLowerCase();

            if (answer.equals("yes") || answer.equals("y")) {
                String weekLabel = getCurrentWeekLabel();
                scheduleDAO.saveSchedule(weekLabel, schedule);
                System.out.println("✓ Schedule saved as: " + weekLabel);
            } else {
                System.out.println("Schedule not saved.");
            }

        } catch (SQLException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  OPTION 4: VIEW SAVED SCHEDULE
    // ─────────────────────────────────────────────────────────────────────────

    private static void viewSavedSchedule() {
        System.out.println("\n--- View Saved Schedule ---");

        try {
            List<String> weekLabels = scheduleDAO.getAllWeekLabels();

            if (weekLabels.isEmpty()) {
                System.out.println("No saved schedules found. Generate and save a schedule first.");
                return;
            }

            System.out.println("\nAvailable saved weeks:");
            for (int i = 0; i < weekLabels.size(); i++) {
                System.out.println("  " + (i + 1) + ". " + weekLabels.get(i));
            }

            int choice = readInt("Select week number: ", 1, weekLabels.size());
            String weekLabel = weekLabels.get(choice - 1);

            List<ScheduledProject> schedule = scheduleDAO.getScheduleByWeek(weekLabel);

            double totalRevenue = schedule.stream()
                    .mapToDouble(sp -> sp.getProject().getRevenue())
                    .sum();

            System.out.println("\nSchedule for: " + weekLabel);
            System.out.printf("%-5s %-12s %-10s %-30s %-15s%n",
                    "Day", "Day Name", "Code", "Title", "Revenue (INR)");
            System.out.println("-".repeat(76));

            for (ScheduledProject sp : schedule) {
                Project p = sp.getProject();
                System.out.printf("%-5d %-12s %-10s %-30s %-15s%n",
                        sp.getAssignedDay(),
                        sp.getDayName(),
                        p.getProjectCode(),
                        truncate(p.getTitle(), 28),
                        formatMoney(p.getRevenue())
                );
            }

            System.out.println("-".repeat(76));
            System.out.printf("Total Revenue: INR %s%n", formatMoney(totalRevenue));

        } catch (SQLException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  OPTION 5: MONTHLY REVENUE SUMMARY  ← NEW FEATURE
    // ─────────────────────────────────────────────────────────────────────────

    private static void viewMonthlyRevenueSummary() {
        System.out.println("\n--- Monthly Revenue Summary ---");
        System.out.println("(Based on all saved weekly schedules)\n");

        try {
            List<MonthlyRevenueSummary> summaries = analyticsSvc.getMonthlyRevenueSummary();

            if (summaries.isEmpty()) {
                System.out.println("No saved schedules found.");
                System.out.println("Tip: Generate a schedule and save it — then this report will populate.");
                return;
            }

            // Table header
            System.out.printf("%-5s %-12s %-8s %-18s %-12s %-10s%n",
                    "Year", "Month", "Weeks", "Total Revenue", "Projects", "Avg/Week");
            System.out.println("-".repeat(70));

            double grandTotal        = 0;
            int    grandProjects     = 0;
            int    grandWeeks        = 0;

            for (MonthlyRevenueSummary s : summaries) {
                double avgPerWeek = s.getWeeksRecorded() > 0
                        ? s.getTotalRevenue() / s.getWeeksRecorded()
                        : 0;

                System.out.printf("%-5d %-12s %-8d %-18s %-12d %-10s%n",
                        s.getYear(),
                        s.getMonthName(),
                        s.getWeeksRecorded(),
                        "INR " + formatMoney(s.getTotalRevenue()),
                        s.getProjectsScheduled(),
                        "INR " + formatMoney(avgPerWeek)
                );

                grandTotal    += s.getTotalRevenue();
                grandProjects += s.getProjectsScheduled();
                grandWeeks    += s.getWeeksRecorded();
            }

            System.out.println("-".repeat(70));
            System.out.printf("%-18s %-8d INR %-15s %-12d%n",
                    "GRAND TOTAL", grandWeeks, formatMoney(grandTotal), grandProjects);
            System.out.println("\nMonths of data available: " + summaries.size());

        } catch (SQLException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  OPTION 6: PREDICT NEXT MONTH REVENUE  ← NEW FEATURE
    // ─────────────────────────────────────────────────────────────────────────

    private static void viewRevenuePrediction() {
        System.out.println("\n--- Predicted Revenue for Next Month ---");

        try {
            List<MonthlyRevenueSummary> summaries = analyticsSvc.getMonthlyRevenueSummary();

            if (summaries.isEmpty()) {
                System.out.println("No historical data available yet.");
                System.out.println("Tip: Save at least 1 weekly schedule to start seeing predictions.");
                return;
            }

            double predicted   = analyticsSvc.predictNextMonthRevenue(summaries);
            String confidence  = analyticsSvc.getPredictionConfidence(summaries.size());

            // Find the most recent month to label "next month"
            MonthlyRevenueSummary latest = summaries.get(summaries.size() - 1);
            String nextMonthLabel        = getNextMonthLabel(latest.getYear(), latest.getMonth());

            System.out.println();
            System.out.println("╔══════════════════════════════════════════════╗");
            System.out.println("║           REVENUE PREDICTION REPORT          ║");
            System.out.println("╚══════════════════════════════════════════════╝");
            System.out.println();
            System.out.println("  Method       : Simple Moving Average");
            System.out.println("  Data Used    : " + summaries.size() + " month(s) of history");
            System.out.println("  Confidence   : " + confidence);
            System.out.println();
            System.out.println("  Past Monthly Revenues Used in Calculation:");

            for (MonthlyRevenueSummary s : summaries) {
                System.out.printf("    %-12s %d  →  INR %s%n",
                        s.getMonthName(), s.getYear(), formatMoney(s.getTotalRevenue()));
            }

            System.out.println();
            System.out.println("  ┌─────────────────────────────────────────┐");
            System.out.printf("  │  Predicted Revenue for %-8s : INR %-12s│%n",
                    nextMonthLabel, formatMoney(predicted));
            System.out.println("  └─────────────────────────────────────────┘");
            System.out.println();

            if (summaries.size() < 3) {
                System.out.println("  ⚠ Note: Prediction reliability improves with more data.");
                System.out.println("    Save more weekly schedules to get better predictions.");
            }

        } catch (SQLException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPER METHODS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Generates a week label for the current date.
     * Format: "Week-2024-03"
     */
    private static String getCurrentWeekLabel() {
        LocalDate    today     = LocalDate.now();
        WeekFields   weekFields = WeekFields.of(Locale.getDefault());
        int          weekNum   = today.get(weekFields.weekOfWeekBasedYear());
        int          year      = today.getYear();
        return String.format("Week-%d-%02d", year, weekNum);
    }

    /**
     * Returns "Month YYYY" for the month after the given year/month.
     */
    private static final String[] MONTH_NAMES = {
        "", "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    };

    private static String getNextMonthLabel(int year, int month) {
        if (month == 12) return "January " + (year + 1);
        return MONTH_NAMES[month + 1] + " " + year;
    }

    /**
     * Reads an integer from the console, repeating until valid input in [min, max].
     */
    private static int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            try {
                String line = scanner.nextLine().trim();
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) return value;
                System.out.println("  Please enter a number between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("  Invalid input. Please enter a whole number.");
            }
        }
    }

    /**
     * Reads a positive double from the console, repeating until valid.
     */
    private static double readDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                String line  = scanner.nextLine().trim();
                double value = Double.parseDouble(line);
                if (value > 0) return value;
                System.out.println("  Revenue must be greater than 0.");
            } catch (NumberFormatException e) {
                System.out.println("  Invalid input. Please enter a number like 150000.");
            }
        }
    }

    /**
     * Formats a number with commas, e.g. 150000.0 → "1,50,000.00"
     * Uses Indian number format (lakhs/crores style).
     */
    private static String formatMoney(double amount) {
        // Split into integer and decimal parts
        long   intPart = (long) amount;
        int    decPart = (int) Math.round((amount - intPart) * 100);

        String intStr = Long.toString(intPart);
        StringBuilder formatted = new StringBuilder();

        // Indian format: last 3 digits, then groups of 2
        if (intStr.length() <= 3) {
            formatted.append(intStr);
        } else {
            // Add the last 3 digits first
            formatted.insert(0, intStr.substring(intStr.length() - 3));
            intStr = intStr.substring(0, intStr.length() - 3);

            // Now add groups of 2 from right to left
            while (intStr.length() > 2) {
                formatted.insert(0, "," + intStr.substring(intStr.length() - 2));
                intStr = intStr.substring(0, intStr.length() - 2);
            }
            if (!intStr.isEmpty()) {
                formatted.insert(0, intStr + ",");
            }
        }

        return formatted + String.format(".%02d", decPart);
    }

    /**
     * Truncates a string to maxLen characters, adding "…" if cut.
     */
    private static String truncate(String str, int maxLen) {
        if (str == null) return "";
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen - 1) + "…";
    }
}
