package com.promanage.service;

import com.promanage.model.Project;
import com.promanage.model.ScheduledProject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SchedulerService {

    // Maps day number (1-5) to its name
    private static final String[] DAY_NAMES = {
        "", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday"
    };

    /**
     * Greedy Job Sequencing Algorithm:
     *
     * 1. Sort projects by revenue descending (highest pays first).
     * 2. For each project, try to assign it to the LATEST free slot
     *    that is still within its deadline.
     * 3. If no free slot exists within the deadline, skip the project.
     *
     * Example:
     *   Projects: A(d=2,rev=100), B(d=1,rev=80), C(d=2,rev=60)
     *   Sorted:   A(100), B(80), C(60)
     *   - A: try slot 2 → free → assign Day 2
     *   - B: try slot 1 → free → assign Day 1
     *   - C: try slot 2 → taken, try slot 1 → taken → skip
     *   Result: Day1=B, Day2=A, Total=180
     */
    public List<ScheduledProject> generateOptimalSchedule(List<Project> projects) {

        // Step 1: Sort by revenue descending
        List<Project> sorted = new ArrayList<>(projects);
        sorted.sort(Comparator.comparingDouble(Project::getRevenue).reversed());

        // Step 2: Initialize 5 slots (index 0 = Day 1, index 4 = Day 5)
        boolean[]          slotTaken = new boolean[5];
        ScheduledProject[] result    = new ScheduledProject[5];

        // Step 3: Assign each project to the best available slot
        for (Project project : sorted) {
            int maxSlot = Math.min(5, project.getDeadline()); // can't go beyond day 5

            // Try from the latest possible day down to day 1
            for (int j = maxSlot - 1; j >= 0; j--) {
                if (!slotTaken[j]) {
                    slotTaken[j] = true;
                    result[j]    = new ScheduledProject(project, j + 1, DAY_NAMES[j + 1]);
                    break;
                }
            }
        }

        // Step 4: Collect assigned projects in day order, skip empty slots
        List<ScheduledProject> schedule = new ArrayList<>();
        for (ScheduledProject sp : result) {
            if (sp != null) {
                schedule.add(sp);
            }
        }

        return schedule;
    }
}
