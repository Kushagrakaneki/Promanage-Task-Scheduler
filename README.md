# ProManage Scheduler

A Java + PostgreSQL project scheduling system for ProManage Solutions Pvt. Ltd.

---

## What This Project Does

- Add client projects with title, deadline, and expected revenue
- Automatically generates unique project codes (PRJ001, PRJ002...)
- Schedules the most profitable projects for the week using a greedy algorithm
- Shows monthly revenue summary from saved schedules
- Predicts next month's revenue using simple moving average

---

## Tech Stack

- Java 17
- PostgreSQL
- Maven (dependency management)
- JDBC (database connection)

---

## Project Structure

```
ProManageScheduler/
├── pom.xml
└── src/main/java/com/promanage/
    ├── Main.java
    ├── config/
    │   └── DatabaseConfig.java
    ├── model/
    │   ├── Project.java
    │   ├── ScheduledProject.java
    │   └── MonthlyRevenueSummary.java
    ├── dao/
    │   ├── ProjectDAO.java
    │   └── ScheduleDAO.java
    ├── service/
    │   ├── SchedulerService.java
    │   └── RevenueAnalyticsService.java
    └── util/
        └── CodeGenerator.java
```

---

## How to Run

**Step 1** — Create the database in pgAdmin:
```sql
CREATE DATABASE promanage_db;
```

**Step 2** — Update your PostgreSQL password in `DatabaseConfig.java`:
```java
private static final String PASSWORD = "your_password_here";
```

**Step 3** — Open the project in IntelliJ, load Maven, then run `Main.java`

Tables are created automatically on first run.

---

## Menu Options

| Option | Feature |
|--------|---------|
| 1 | Add New Project |
| 2 | View All Projects |
| 3 | Generate Optimal Weekly Schedule |
| 4 | View Saved Schedule |
| 5 | Monthly Revenue Summary |
| 6 | Predict Next Month Revenue |
| 7 | Exit |

---

## Scheduling Algorithm

Uses the **Job Sequencing with Deadlines** greedy algorithm.

- Sorts projects by revenue (highest first)
- Assigns each project to the latest available day within its deadline
- Maximizes total weekly revenue
- Maximum 5 projects per week, 1 project per day

Example:
```
Projects: A(deadline=2, revenue=200), B(deadline=1, revenue=150), C(deadline=2, revenue=100)

Sorted by revenue: A → B → C

A → assigned to Day 2 (latest free slot within deadline 2)
B → assigned to Day 1 (latest free slot within deadline 1)
C → Day 1 and Day 2 both taken → skipped

Result: Day1=B, Day2=A | Total Revenue = 350
```

---

## Revenue Prediction

Uses **Simple Moving Average** based on saved weekly schedules.

```
Formula: Predicted Revenue = Sum of all monthly revenues / Number of months

Example:
January  = 7,00,000
February = 8,00,000
March    = 6,00,000

Prediction for April = (7,00,000 + 8,00,000 + 6,00,000) / 3 = 7,00,000
```

More months of saved data = higher prediction confidence.

---

## Database Tables

**projects**
| Column | Type | Description |
|--------|------|-------------|
| project_id | SERIAL | Auto generated primary key |
| project_code | VARCHAR | Auto generated (PRJ001, PRJ002...) |
| title | VARCHAR | Project name |
| deadline | INT | 1 to 5 working days |
| revenue | DECIMAL | Expected revenue in INR |
| created_at | TIMESTAMP | When project was added |

**schedules**
| Column | Type | Description |
|--------|------|-------------|
| schedule_id | SERIAL | Auto generated primary key |
| week_label | VARCHAR | e.g. Week-2026-09 |
| project_id | INT | Foreign key to projects |
| assigned_day | INT | 1 to 5 |
| day_name | VARCHAR | Monday to Friday |
| created_at | TIMESTAMP | When schedule was saved |

---

## Author

ProManage Solutions Pvt. Ltd. — Automated Scheduling System
