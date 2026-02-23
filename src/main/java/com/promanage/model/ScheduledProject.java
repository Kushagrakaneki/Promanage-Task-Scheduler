package com.promanage.model;

public class ScheduledProject {

    private Project project;
    private int     assignedDay;
    private String  dayName;

    public ScheduledProject(Project project, int assignedDay, String dayName) {
        this.project     = project;
        this.assignedDay = assignedDay;
        this.dayName     = dayName;
    }

    public Project getProject()               { return project; }
    public void setProject(Project project)   { this.project = project; }

    public int getAssignedDay()                 { return assignedDay; }
    public void setAssignedDay(int assignedDay) { this.assignedDay = assignedDay; }

    public String getDayName()               { return dayName; }
    public void setDayName(String dayName)   { this.dayName = dayName; }
}
