package com.promanage.model;

public class MonthlyRevenueSummary {

    private int    year;
    private int    month;
    private String monthName;
    private double totalRevenue;
    private int    projectsScheduled;
    private int    weeksRecorded;

    public MonthlyRevenueSummary(int year, int month, String monthName,
                                  double totalRevenue, int projectsScheduled, int weeksRecorded) {
        this.year              = year;
        this.month             = month;
        this.monthName         = monthName;
        this.totalRevenue      = totalRevenue;
        this.projectsScheduled = projectsScheduled;
        this.weeksRecorded     = weeksRecorded;
    }

    public int    getYear()               { return year; }
    public int    getMonth()              { return month; }
    public String getMonthName()          { return monthName; }
    public double getTotalRevenue()       { return totalRevenue; }
    public int    getProjectsScheduled()  { return projectsScheduled; }
    public int    getWeeksRecorded()      { return weeksRecorded; }
}
