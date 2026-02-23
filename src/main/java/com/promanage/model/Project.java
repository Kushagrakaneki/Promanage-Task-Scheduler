package com.promanage.model;

import java.sql.Timestamp;

public class Project {

    private int       projectId;
    private String    projectCode;
    private String    title;
    private int       deadline;
    private double    revenue;
    private Timestamp createdAt;

    // --- constructors ---

    public Project() {}

    public Project(String projectCode, String title, int deadline, double revenue) {
        this.projectCode = projectCode;
        this.title       = title;
        this.deadline    = deadline;
        this.revenue     = revenue;
    }

    // --- getters & setters ---

    public int getProjectId()           { return projectId; }
    public void setProjectId(int id)    { this.projectId = id; }

    public String getProjectCode()             { return projectCode; }
    public void setProjectCode(String code)    { this.projectCode = code; }

    public String getTitle()              { return title; }
    public void setTitle(String title)    { this.title = title; }

    public int getDeadline()                { return deadline; }
    public void setDeadline(int deadline)   { this.deadline = deadline; }

    public double getRevenue()              { return revenue; }
    public void setRevenue(double revenue)  { this.revenue = revenue; }

    public Timestamp getCreatedAt()                  { return createdAt; }
    public void setCreatedAt(Timestamp createdAt)    { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return String.format("Project{code='%s', title='%s', deadline=%d, revenue=%.2f}",
                projectCode, title, deadline, revenue);
    }
}
