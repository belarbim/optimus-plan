# Optimus Plan — Application Documentation

## Table of Contents

1. [Application Description](#1-application-description)
2. [Business Use Cases](#2-business-use-cases)
   - 2.1 [Team Management](#21-team-management)
   - 2.2 [Employee Management](#22-employee-management)
   - 2.3 [Assignment Management](#23-assignment-management)
   - 2.4 [Capacity Planning](#24-capacity-planning)
   - 2.5 [Grade Management](#25-grade-management)
   - 2.6 [Role Type Configuration](#26-role-type-configuration)
   - 2.7 [Team Type & Category Configuration](#27-team-type--category-configuration)
   - 2.8 [Working Days Configuration](#28-working-days-configuration)
   - 2.9 [Public Holidays Management](#29-public-holidays-management)
   - 2.10 [Capacity Snapshots](#210-capacity-snapshots)
   - 2.11 [Capacity Alerts](#211-capacity-alerts)
   - 2.12 [Audit Log](#212-audit-log)
3. [Business Rules & Computation Rules](#3-business-rules--computation-rules)
4. [Out of Scope](#4-out-of-scope)
5. [Technical Description](#5-technical-description)
   - 5.1 [Technology Stack](#51-technology-stack)
   - 5.2 [Architecture](#52-architecture)
   - 5.3 [Entity Relationship Schema](#53-entity-relationship-schema)
   - 5.4 [API Endpoints](#54-api-endpoints)
   - 5.5 [Sequence Diagrams](#55-sequence-diagrams)
   - 5.6 [Infrastructure & Deployment](#56-infrastructure--deployment)

---

## 1. Application Description

**Optimus Plan** is a capacity management platform designed for professional services and engineering organisations. It enables team managers and resource planners to:

- Structure their workforce into a two-level team hierarchy
- Assign employees to teams with a defined allocation percentage and role
- Compute how many available man-days a team has for a given month
- Track how capacity breaks down by category (e.g. incident handling, project work, continuous improvement)
- Simulate "what-if" scenarios before committing to headcount changes
- Maintain a full historical record of cost, grade, and employment-type changes per employee
- Generate point-in-time capacity snapshots for reporting and trend analysis
- Audit every data change with actor and timestamp

The system follows a clean hexagonal architecture with a Spring Boot 3 / Java 21 backend, a PostgreSQL database managed by Liquibase, and an Angular 21 single-page application that consumes the REST API.

---

## 2. Business Use Cases

---

### 2.1 Team Management

#### Description
A **Team** is the basic organisational unit. Teams exist in a two-level hierarchy: root (parent) teams and sub-teams. Each team may be linked to a **Team Type**, which defines the category template the team uses for capacity allocation.

#### Normal Cases

| # | Action | Actor | Outcome |
|---|--------|-------|---------|
| 1 | Create a root team | Manager | A team with no parent is created; audited |
| 2 | Create a sub-team | Manager | A team with a parent reference is created; audited |
| 3 | Assign / change a team type | Manager | The team is linked to the type; future capacity calculations use the type's categories |
| 4 | Rename a team | Manager | Name updated; audited |
| 5 | Delete a team | Manager | Team removed from the system |
| 6 | View team hierarchy | Manager | Tree view returned with children nested under parents |
| 7 | Bulk import teams | Manager | CSV with columns `name, parentName, teamTypeName` creates many teams in one operation |

#### Edge Cases

| # | Situation | Behaviour |
|---|-----------|-----------|
| E1 | Create a root team whose name already exists at root level | Rejected — name must be unique per parent scope |
| E2 | Create a sub-team under another sub-team | Rejected — max depth is two levels (root -> sub) |
| E3 | Delete a team that has sub-teams | Rejected — must delete or re-parent children first |
| E4 | Delete a team that has active assignments | Rejected — employees must be unassigned first |
| E5 | CSV import row references a parent that does not exist | That row fails with an error; other rows continue |
| E6 | CSV import row references a team type that does not exist | That row fails with an error |

---

### 2.2 Employee Management

#### Description
An **Employee** is a person who can be assigned to teams. Employees carry identity information (first name, last name, email) and an employment type (INTERNAL or EXTERNAL). History tables record all type, grade, and cost changes over time.

#### Normal Cases

| # | Action | Actor | Outcome |
|---|--------|-------|---------|
| 1 | Create an employee | Manager | Employee record created with default type INTERNAL; audited |
| 2 | Create an EXTERNAL employee | Manager | Employee created with type EXTERNAL |
| 3 | Update employee details | Manager | Name, email, or type updated; audited |
| 4 | Delete an employee | Manager | Employee removed from the system |
| 5 | Record a type change | Manager | An `EmployeeTypeHistory` entry is appended with the new type and effective date |
| 6 | Assign a grade | Manager | An `EmployeeGradeHistory` entry is appended; it links to a `Grade` and inherits its daily cost |
| 7 | Assign a direct cost | Manager | An `EmployeeCostHistory` entry is appended with a daily cost and effective date |
| 8 | Bulk import employees | Manager | CSV (`firstName, lastName, email, type, typeEffectiveFrom`) creates employees and seeds their initial type history |
| 9 | View type history | Manager | Chronological list of all type changes |
| 10 | View grade history | Manager | Chronological list of all grade assignments |
| 11 | View cost history | Manager | Chronological list of all direct cost assignments |
| 12 | Edit / delete a history entry | Manager | The specific entry is updated or removed |

#### Edge Cases

| # | Situation | Behaviour |
|---|-----------|-----------|
| E1 | Create an employee with an email already in the system | Rejected — email must be globally unique |
| E2 | Delete an employee who has active assignments | Rejected — assignments must be closed first |
| E3 | CSV import row with a duplicate email | Silently skipped (counted as "skipped"); does not block other rows |
| E4 | CSV row with an invalid or missing `typeEffectiveFrom` | Defaults to today |
| E5 | CSV row with a missing `type` column | Defaults to INTERNAL |
| E6 | Add a type history entry on a date that already has an entry | Rejected — unique constraint on `(employeeId, effectiveFrom)` |
| E7 | Grade history and cost history are always visible | Both sections shown in the UI regardless of current employee type |

---

### 2.3 Assignment Management

#### Description
An **Assignment** links an employee to a team for a period of time. It specifies how much of the employee's time is dedicated to that team (`allocation_pct`) and what role the employee plays (`role_type`, `role_weight`). Role changes within the same assignment are tracked via an immutable **Role History**.

#### Normal Cases

| # | Action | Actor | Outcome |
|---|--------|-------|---------|
| 1 | Assign an employee to a team | Manager | Assignment created with start date, allocation %, role type and weight; first role history entry created |
| 2 | Update assignment details | Manager | Allocation, role, or dates changed; audited |
| 3 | End an assignment | Manager | `end_date` set to the given date; current role history segment closed |
| 4 | Update allocation % only | Manager | Only `allocation_pct` changed; does not affect role history |
| 5 | Change an employee's role | Manager | Current role history segment closed (`effectiveTo` set); new segment opened with new role and weight |
| 6 | Delete an assignment | Manager | Assignment and all its role history removed |
| 7 | Bulk import assignments | Manager | CSV with employee email, team name, allocation, role, dates |
| 8 | View role history for an assignment | Manager | Chronological list of role segments with start/end dates |

#### Edge Cases

| # | Situation | Behaviour |
|---|-----------|-----------|
| E1 | Assign an employee who already has an assignment to the same team starting on the same date | Rejected — unique on `(team_id, employee_id, start_date)` |
| E2 | Set `allocation_pct` below 1 or above 100 | Rejected — must be 1–100 |
| E3 | Set `role_weight` outside 0–1 | Rejected — must be 0–1 |
| E4 | End date before start date | Rejected |
| E5 | Delete an assignment that has role history | Cascades — role history deleted automatically |
| E6 | Role change with an `effectiveFrom` before the assignment start date | Business rule violation |
| E7 | CSV import row with an employee email not found | Row fails; other rows continue |

---

### 2.4 Capacity Planning

#### Description
Capacity planning is the core analytical feature. Given a team and a month, the system calculates the total available **man-days** for that team. The calculation accounts for each employee's allocation, role effort weight, how much of the month they were active, and public holidays / working-day overrides.

Four query modes are available:

| Mode | Description |
|------|-------------|
| **Monthly capacity** | Full breakdown for a team and month |
| **Remaining capacity** | Pro-rata capacity from a given date to end of month |
| **Rollup** | Aggregated capacity across a team and all its sub-teams |
| **Simulation** | "What-if" — add, remove, or modify assignments and see the projected capacity |

#### Normal Cases

| # | Action | Actor | Outcome |
|---|--------|-------|---------|
| 1 | Compute monthly capacity | Manager | Returns total man-days, per-employee contributions, and category breakdown |
| 2 | Compute remaining capacity | Manager | Returns remaining man-days from a date to month-end, with remaining business days |
| 3 | Compute rollup capacity | Manager | Returns own capacity + aggregated sub-team capacities |
| 4 | Run a simulation | Manager | Returns baseline capacity vs. projected capacity with per-category deltas and warnings |

#### Edge Cases

| # | Situation | Behaviour |
|---|-----------|-----------|
| E1 | No assignments for the team in the queried month | Total capacity = 0; empty contributions list |
| E2 | No working-days override and no holidays | Standard calendar business days used |
| E3 | Query date is a weekend or holiday | `adjustedDate` advances to the next business day |
| E4 | Team has no category allocations defined | Total capacity returned; category breakdown is empty |
| E5 | Simulate removing an employee not assigned to the team | Warning returned; simulation proceeds |
| E6 | Rollup query on a leaf team (no sub-teams) | Returns own capacity only; `subTeamCapacities` is empty |

---

### 2.5 Grade Management

#### Description
A **Grade** represents a seniority or pay band (e.g. "Junior", "Senior", "Lead"). Each grade has an associated daily cost that can evolve over time. The complete history of cost changes is tracked via **Grade Cost History**.

#### Normal Cases

| # | Action | Actor | Outcome |
|---|--------|-------|---------|
| 1 | Create a grade with an initial cost | Manager | Grade created; first `GradeCostHistory` entry seeded with the given or today's effective date |
| 2 | Update grade name | Manager | Name changed; cost history unaffected |
| 3 | Add a new cost rate | Manager | New `GradeCostHistory` entry appended; `grade.daily_cost` cache updated |
| 4 | Edit a cost history entry | Manager | Specific entry updated (daily cost and/or effective date) |
| 5 | Delete a cost history entry | Manager | Entry removed |
| 6 | Delete a grade | Manager | Grade and all its cost history removed |

#### Edge Cases

| # | Situation | Behaviour |
|---|-----------|-----------|
| E1 | Create a grade whose name already exists | Rejected — grade names are unique |
| E2 | Add a cost history entry on a date already used for that grade | Rejected — unique on `(grade_id, effective_from)` |
| E3 | Delete a grade that is referenced in employee grade history | Depends on referential integrity; employee history retains the grade reference |

---

### 2.6 Role Type Configuration

#### Description
A **Role Type** is a named category of work (e.g. "Tech Lead", "Developer", "Scrum Master") with a **default weight** (0–1) that acts as an effort multiplier in capacity calculations. Managers configure role types globally; individual assignments can override the weight.

#### Normal Cases

| # | Action | Actor | Outcome |
|---|--------|-------|---------|
| 1 | Create a role type | Manager | New role type available for assignment |
| 2 | Update a role type | Manager | Name, weight, or description changed |
| 3 | Delete a role type | Manager | Role type removed |
| 4 | List all role types | Manager | Full list returned for selection in assignment forms |

#### Edge Cases

| # | Situation | Behaviour |
|---|-----------|-----------|
| E1 | Create a role type with a name already in use | Rejected — `role_type` is unique |
| E2 | Set `defaultWeight` outside 0–1 | Rejected |
| E3 | Delete a role type that is referenced in active role history | Rejected — use-check before deletion |

---

### 2.7 Team Type & Category Configuration

#### Description
A **Team Type** is a reusable template that defines what **categories** of work a team tracks (e.g. "Incident", "Project", "CI/CD"). Each category is flagged as either part of **total capacity** (overhead, deducted first) or **remaining capacity** (split from what is left after overhead).

Teams link to a team type and then set a percentage allocation per category. The capacity calculator uses these settings to produce the category breakdown.

#### Normal Cases

| # | Action | Actor | Outcome |
|---|--------|-------|---------|
| 1 | Create a team type with categories | Manager | Team type created with all its category definitions |
| 2 | Update a team type | Manager | Name or categories updated |
| 3 | Delete a team type | Manager | Team type removed |
| 4 | Set category allocations for a team | Manager | All category rows for the team replaced atomically |

#### Edge Cases

| # | Situation | Behaviour |
|---|-----------|-----------|
| E1 | Create a team type with a name already in use | Rejected |
| E2 | A category flagged as both total and remaining capacity | Rejected at DB level by a `CHECK` constraint |
| E3 | Team has no team type — capacity queried | Capacity calculated without category breakdown |
| E4 | Category allocations don't sum to 100% | Allowed — partial allocation is intentional in some configurations |

---

### 2.8 Working Days Configuration

#### Description
The system needs to know how many working days a month contains. By default it counts calendar business days (Monday–Friday) minus public holidays. The **Working Days Config** lets managers override this per month (to account for company-wide training days, reduced hours periods, etc.).

#### Normal Cases

| # | Action | Actor | Outcome |
|---|--------|-------|---------|
| 1 | Bulk import from CSV | Manager | CSV with `month (yyyy-MM)` and `avgDaysWorked` upserts each row |
| 2 | Manually set a month's value | Manager | Single upsert for one month |
| 3 | List all configured months | Manager | Full list of overrides returned |
| 4 | Filter by year | Manager | Only entries for the requested year returned |

#### Edge Cases

| # | Situation | Behaviour |
|---|-----------|-----------|
| E1 | Import a month that already has an override | Existing value replaced (upsert) |
| E2 | `avgDaysWorked` <= 0 | Rejected by `CHECK` constraint |
| E3 | No override for a queried month | System falls back to calendar business days minus holidays |
| E4 | CSV missing `month` or `avgDaysWorked` columns | Validation error returned |

---

### 2.9 Public Holidays Management

#### Description
**Public Holidays** are dates excluded from business day counts. A holiday can be **one-time** (e.g. "Queen's Jubilee 2022-06-02") or **recurring** (e.g. "Christmas Day" on Dec 25 every year).

#### Normal Cases

| # | Action | Actor | Outcome |
|---|--------|-------|---------|
| 1 | Add a public holiday | Manager | Holiday recorded; immediately affects capacity calculations |
| 2 | Mark holiday as recurring | Manager | The holiday repeats on the same month/day every subsequent year |
| 3 | Update a holiday | Manager | Date, name, or recurring flag changed |
| 4 | Delete a holiday | Manager | Holiday removed; capacity recalculated on next query |
| 5 | List holidays by year | Manager | All one-time holidays in that year plus recurring holidays projected for that year |

#### Edge Cases

| # | Situation | Behaviour |
|---|-----------|-----------|
| E1 | Add a holiday on a date already registered | Rejected — date is unique |
| E2 | Holiday falls on a weekend | Accepted; has no net effect on business day counts (weekend days already excluded) |

---

### 2.10 Capacity Snapshots

#### Description
A **Snapshot** freezes the capacity calculation for a team and month at a point in time. Snapshots are useful for dashboards, trend analysis, and detecting drift between planned and actual capacity.

#### Normal Cases

| # | Action | Actor | Outcome |
|---|--------|-------|---------|
| 1 | Generate snapshot for one team | Manager | Capacity computed and stored per category; previous snapshot for same team/month replaced |
| 2 | Generate snapshots for all teams | System / Manager | Snapshot generated for every team; failures on individual teams do not abort the batch |
| 3 | Query snapshots for a team over a date range | Manager | All stored snapshots between `from` and `to` months returned |

#### Edge Cases

| # | Situation | Behaviour |
|---|-----------|-----------|
| E1 | Generate snapshot for a team with no assignments | Snapshot created with zero man-days |
| E2 | Regenerate an existing snapshot | Previous snapshot for that team/month deleted and replaced |
| E3 | One team fails during `generateAll` | Error logged for that team; other teams proceed normally |

---

### 2.11 Capacity Alerts

#### Description
A **Capacity Alert** sets a minimum threshold (in man-days) for a team. When remaining capacity drops below the threshold, the alert is active. Currently, alerts are stored and queryable; notification delivery (email, Slack, etc.) is not implemented.

#### Normal Cases

| # | Action | Actor | Outcome |
|---|--------|-------|---------|
| 1 | Create or update an alert for a team | Manager | Upsert — one alert per team; threshold and enabled flag set |
| 2 | Disable an alert | Manager | `enabled = false`; alert retained but considered inactive |
| 3 | Delete an alert | Manager | Alert removed |
| 4 | Query alert for a team | Manager | Returns the current alert configuration |

#### Edge Cases

| # | Situation | Behaviour |
|---|-----------|-----------|
| E1 | Create two alerts for the same team | Not possible — unique constraint on `team_id`; second creation updates the first (upsert) |
| E2 | `thresholdManDays` <= 0 | Rejected |

---

### 2.12 Audit Log

#### Description
Every create, update, and delete operation on core entities is automatically written to an **Audit Log** with the entity type, entity ID, action, a JSON change map, the actor, and a timestamp. The log is append-only and queryable with filters.

#### Normal Cases

| # | Action | Actor | Outcome |
|---|--------|-------|---------|
| 1 | Any entity mutation | System | Audit entry appended automatically |
| 2 | Query audit log | Manager | Paginated results filtered by entity type, action, date range |

#### Edge Cases

| # | Situation | Behaviour |
|---|-----------|-----------|
| E1 | Query with no filters | All entries returned, paginated |
| E2 | Query for an entity ID with no entries | Empty page returned |

---

## 3. Business Rules & Computation Rules

### 3.1 Capacity Calculation

The central computation is performed by `CapacityCalculator` (pure domain service, no Spring dependencies). For each assignment active in the queried month:

```
employeeManDays = avgDaysWorked
                * (allocationPct / 100)
                * blendedRoleWeight
                * presenceFactor
```

**Variable definitions:**

| Variable | Definition |
|----------|------------|
| `avgDaysWorked` | Working days in the month. If a `WorkingDaysConfig` override exists for that month it is used; otherwise `BusinessDayCalculator` counts Monday–Friday days minus public holidays. |
| `allocationPct` | The employee's time dedicated to this team, as a percentage (1–100). Divided by 100 to get a fraction. |
| `blendedRoleWeight` | Time-weighted average of the role weights across all role history segments that overlap the month (see Section 3.2). |
| `presenceFactor` | The fraction of business days in the month during which the assignment was active: `activeDays / totalBusinessDays`. The assignment's effective period is clamped to `[monthStart, monthEnd]`. |

**Total team capacity:**

```
totalCapacity = SUM employeeManDays  (over all active assignments)
```

Result is rounded to 3 decimal places (HALF_UP).

### 3.2 Blended Role Weight

When an employee changes role mid-month, the system calculates a **time-weighted (business-day-weighted) average** of all role segments that overlap the month:

```
blendedRoleWeight = SUM (roleWeight_i * businessDays_i)  /  totalBusinessDays
```

- Each role history segment is clamped to `[monthStart, monthEnd]`.
- Adjacent segments: the end of segment `i` is capped at `effectiveFrom` of segment `i+1` minus one day to avoid double-counting.
- If no role history exists, weight defaults to `1.0` (full weight).

### 3.3 Category Breakdown

Once `totalCapacity` is known, it is split by category:

**Step 1 — Overhead categories** (`isPartOfTotalCapacity = true`):

```
overheadManDays_category = totalCapacity * (allocationPct / 100)
```

**Step 2 — Remaining pool:**

```
remainingCapacity = totalCapacity - SUM overheadManDays
                  (floored at 0)
```

**Step 3 — Remaining-capacity categories** (`isPartOfRemainingCapacity = true`):

```
manDays_category = remainingCapacity * (allocationPct / 100)
```

Example (total = 20 man-days, Incident 20% overhead, Project 60% remaining, CI 40% remaining):

```
Incident   = 20 * 0.20           = 4.000  man-days  (overhead)
remaining  = 20 - 4              = 16.000 man-days
Project    = 16 * 0.60           = 9.600  man-days
CI         = 16 * 0.40           = 6.400  man-days
```

### 3.4 Remaining Capacity (Pro-rata)

For `computeRemaining(teamId, date)`:

1. If `date` is a weekend or holiday, it is advanced to the next business day (`adjustedDate`).
2. `remainingBusinessDays` = business days from `adjustedDate` to end of month (inclusive).
3. `totalBusinessDays` = business days in the full month.
4. `presenceFactor` for each active assignment is recalculated using only `[adjustedDate, monthEnd]`.
5. The category breakdown uses the same overhead/remaining formula applied to the pro-rata total.

### 3.5 History Resolution (Effective-Date Pattern)

All history tables (`employee_type_history`, `employee_grade_history`, `employee_cost_history`, `grade_cost_history`, `role_history`) follow the same pattern:

- Records are **immutable and append-only** (no UPDATE on historical data).
- **Current value** = the record with the highest `effectiveFrom` that is <= today.
- **Unique constraint** on `(entityId, effectiveFrom)` prevents duplicate entries for the same date.
- `effectiveTo` (where present) is used only in `role_history`; other tables infer it from the next record's `effectiveFrom`.

### 3.6 Assignment Overlap Detection

The assignment repository query `findActiveByTeamIdAndMonth(teamId, from, to)` returns all assignments that overlap the queried period:

```sql
start_date <= monthEnd  AND  (end_date IS NULL OR end_date >= monthStart)
```

### 3.7 Business Day Counting

`BusinessDayCalculator.countBusinessDays(start, end, holidays)` iterates every day in `[start, end]` inclusive and counts days that satisfy:

```
dayOfWeek NOT IN (SATURDAY, SUNDAY)
AND date NOT IN holidays (accounting for recurring holidays by month+day)
```

For **recurring holidays**, the check is `holiday.monthDay == date.monthDay` (ignoring year).

### 3.8 Employee Deletion Guard

An employee cannot be deleted if `hasActiveAssignmentsByEmployeeId` returns true. "Active" is defined as: `end_date IS NULL OR end_date >= today`.

### 3.9 Team Deletion Guard

A team cannot be deleted if:
- It has at least one child team (`hasChildren`), OR
- It has at least one active assignment (`hasActiveAssignmentsByTeamId`).

---

## 4. Out of Scope

The following features are **not** implemented in the current version:

| Topic | Detail |
|-------|--------|
| **Authentication & Authorisation** | There is no login, user management, or role-based access control. All API endpoints are publicly accessible. The `actor` field in audit logs is hardcoded to `"manager"`. |
| **Alert Notifications** | Capacity alerts are stored and queryable but no notification is triggered (no email, Slack, webhook, etc.). |
| **Financial Reporting** | Although daily costs and grade histories are stored, no cost roll-up reports, budgets, or financial forecasting features are implemented. |
| **Leave / Absence Management** | Individual employee leave (annual leave, sick leave) is not tracked. Working day overrides at the month level are available but per-employee absence is outside scope. |
| **Multi-tenancy** | The system is a single-tenant application. There is no concept of separate organisations or workspaces. |
| **Real-time Collaboration** | No WebSocket or server-sent event support; the UI requires manual refresh to see changes made by other users. |
| **Notifications / Email** | No email integration of any kind. |
| **Resource Forecasting beyond simulation** | The simulation feature covers one month. Multi-month forecasting or headcount planning workflows are not supported. |
| **Integration with HR / Payroll systems** | Data is entered manually or via CSV import. There is no connector to external HR platforms (Workday, SAP, etc.). |
| **Mobile Application** | No native mobile app; the Angular SPA is a desktop-first web application. |
| **Export / Reporting formats** | No PDF, Excel, or BI tool export is provided. Snapshots can be queried via the API for downstream use. |
| **Capacity allocation > 100 % per employee** | The system does not validate nor prevent an employee's total allocation across all teams from exceeding 100 %. That check is the manager's responsibility. |

---

## 5. Technical Description

### 5.1 Technology Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Backend language | Java | 21 (virtual threads enabled) |
| Backend framework | Spring Boot | 3.2.5 |
| Persistence | Spring Data JPA / Hibernate | via Spring Boot BOM |
| Database | PostgreSQL | 17 |
| DB migrations | Liquibase | via Spring Boot BOM |
| CSV parsing | OpenCSV | 5.9 |
| Code generation | Lombok | latest |
| Build tool | Maven | 3.x |
| Frontend framework | Angular | 21.2.5 |
| UI component library | ng-zorro-antd (Ant Design) | 21.2.0 |
| Charts | @swimlane/ngx-charts | 23.1.0 |
| HTTP client | Angular HttpClient + RxJS | via Angular BOM |
| Container runtime | Docker / Docker Compose | — |
| DB admin GUI | Adminer | 4 |

---

### 5.2 Architecture

The backend follows **Hexagonal Architecture (Ports & Adapters)**:

```
┌────────────────────────────────────────────────────────────────────┐
│  Driving Side (HTTP)                                               │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  REST Controllers  (/api/*)          GlobalExceptionHandler  │  │
│  └───────────────────────────┬──────────────────────────────────┘  │
└──────────────────────────────│─────────────────────────────────────┘
                               │ calls Inbound Ports (UseCases)
┌──────────────────────────────v─────────────────────────────────────┐
│  Application Layer                                                  │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  *ApplicationService  (one per aggregate)                    │  │
│  │  CapacityCalculator   (pure domain service)                  │  │
│  └───────────────────────────┬──────────────────────────────────┘  │
└──────────────────────────────│─────────────────────────────────────┘
                               │ calls Outbound Ports (Repositories)
┌──────────────────────────────v─────────────────────────────────────┐
│  Infrastructure — Persistence                                       │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  JPA Entities  ->  Spring Data Repositories                   │  │
│  │  *PersistenceAdapter  (implements RepositoryPort)            │  │
│  └──────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────┘
```

**Key packages:**

```
com.utmost.optimusplan
├── domain
│   ├── model/          <- pure domain entities (no JPA annotations)
│   ├── port/
│   │   ├── in/         <- use case interfaces (inbound ports)
│   │   └── out/        <- repository interfaces (outbound ports)
│   ├── service/        <- CapacityCalculator, BusinessDayCalculator
│   └── exception/      <- DomainException, DomainError (sealed)
├── application
│   ├── employee/       <- EmployeeApplicationService
│   ├── team/           <- TeamApplicationService
│   ├── assignment/     <- AssignmentApplicationService
│   ├── capacity/       <- CapacityApplicationService
│   └── ...             <- one package per aggregate
└── infrastructure
    └── adapter
        ├── in/web/     <- Spring MVC controllers
        └── out/jpa/    <- JPA entities, repositories, adapters
```

---

### 5.3 Entity Relationship Schema

```
┌─────────────────┐        ┌──────────────────────────┐
│   team_type      │        │   team_type_category     │
│─────────────────│        │──────────────────────────│
│ id (PK)         │───┐    │ id (PK)                  │
│ name            │   └───>│ team_type_id (FK)        │
│ created_at      │        │ name                     │
│ updated_at      │        │ is_part_of_total_capacity│
└─────────────────┘        │ is_part_of_remaining_cap │
                           └──────────────────────────┘
                                   ▲
┌─────────────────┐                │
│   team           │────────────────┘  (team.team_type_id -> team_type.id)
│─────────────────│
│ id (PK)         │<────────────────────────────────────────────┐
│ name            │  (self-join: parent_id -> team.id)           │
│ parent_id (FK?) │────────────────────────────────────────────>│
│ team_type_id(FK)│
│ created_at      │
│ updated_at      │
└────────┬────────┘
         │
         │ 1:N
         v
┌──────────────────────────┐    ┌──────────────────────┐
│   team_assignment         │    │   employee            │
│──────────────────────────│    │──────────────────────│
│ id (PK)                  │    │ id (PK)              │
│ team_id (FK) ────────────┘    │ first_name           │
│ employee_id (FK) ─────────────> last_name            │
│ allocation_pct (1–100)   │    │ email (UNIQUE)       │
│ start_date               │    │ type                 │
│ end_date                 │    │ created_at           │
│ created_at               │    │ updated_at           │
│ updated_at               │    └──────────────────────┘
└──────────┬───────────────┘           │
           │ 1:N                       │ 1:N (history)
           v                           v
┌──────────────────────┐   ┌──────────────────────────────┐
│   role_history        │   │   employee_type_history      │
│──────────────────────│   │──────────────────────────────│
│ id (PK)              │   │ id (PK)                      │
│ assignment_id (FK)   │   │ employee_id (FK)             │
│ role_type            │   │ type                         │
│ role_weight (0–1)    │   │ effective_from               │
│ effective_from       │   │ created_at                   │
│ effective_to         │   └──────────────────────────────┘
└──────────────────────┘
                           ┌──────────────────────────────┐
                           │   employee_grade_history     │
                           │──────────────────────────────│
                           │ id (PK)                      │
                           │ employee_id (FK)             │
                           │ grade_id (FK) ───────────────┐
                           │ effective_from               │
                           │ created_at                   │
                           └──────────────────────────────┘
                                                          │
                           ┌──────────────────────────────┤
                           │   employee_cost_history      │
                           │──────────────────────────────│
                           │ id (PK)                      │
                           │ employee_id (FK)             │
                           │ daily_cost                   │
                           │ effective_from               │
                           │ created_at                   │
                           └──────────────────────────────┘

┌─────────────────┐        ┌──────────────────────────────┐
│   grade          │        │   grade_cost_history         │
│─────────────────│        │──────────────────────────────│
│ id (PK)         │───────>│ id (PK)                      │
│ name (UNIQUE)   │        │ grade_id (FK)                │
│ daily_cost      │        │ daily_cost                   │
│ created_at      │        │ effective_from               │
│ updated_at      │        │ created_at                   │
└─────────────────┘        └──────────────────────────────┘

┌────────────────────────────┐
│   role_type_config          │
│────────────────────────────│
│ id (PK)                    │
│ role_type (UNIQUE)         │
│ default_weight (0–1)       │
│ description                │
│ created_at / updated_at    │
└────────────────────────────┘

┌────────────────────────┐   ┌──────────────────────────┐
│   category_allocation   │   │   capacity_snapshot      │
│────────────────────────│   │──────────────────────────│
│ id (PK)                │   │ id (PK)                  │
│ team_id (FK)           │   │ team_id (FK)             │
│ category_name          │   │ snapshot_month (yyyy-MM) │
│ allocation_pct         │   │ category_name            │
│ created_at / updated_at│   │ capacity_man_days        │
└────────────────────────┘   │ created_at               │
                             └──────────────────────────┘

┌──────────────────────────┐  ┌──────────────────────────┐
│   working_days_config     │  │   public_holiday         │
│──────────────────────────│  │──────────────────────────│
│ id (PK)                  │  │ id (PK)                  │
│ month (UNIQUE, yyyy-MM)  │  │ date (UNIQUE)            │
│ avg_days_worked          │  │ name                     │
│ imported_at              │  │ recurring (bool)         │
└──────────────────────────┘  │ created_at               │
                              └──────────────────────────┘

┌──────────────────────────┐  ┌──────────────────────────┐
│   capacity_alert          │  │   audit_log              │
│──────────────────────────│  │──────────────────────────│
│ id (PK)                  │  │ id (PK)                  │
│ team_id (FK, UNIQUE)     │  │ entity_type              │
│ threshold_man_days       │  │ entity_id                │
│ enabled (bool)           │  │ action                   │
│ created_at / updated_at  │  │ changes (JSONB)          │
└──────────────────────────┘  │ actor                    │
                              │ timestamp                │
                              └──────────────────────────┘
```

**Unique constraints summary:**

| Table | Unique Key |
|-------|-----------|
| `team` | `(name, parent_id)` |
| `employee` | `email` |
| `grade` | `name` |
| `team_type` | `name` |
| `role_type_config` | `role_type` |
| `team_assignment` | `(team_id, employee_id, start_date)` |
| `employee_type_history` | `(employee_id, effective_from)` |
| `employee_grade_history` | `(employee_id, effective_from)` |
| `employee_cost_history` | `(employee_id, effective_from)` |
| `grade_cost_history` | `(grade_id, effective_from)` |
| `category_allocation` | `(team_id, category_name)` |
| `capacity_snapshot` | `(team_id, snapshot_month, category_name)` |
| `capacity_alert` | `team_id` |
| `working_days_config` | `month` |
| `public_holiday` | `date` |

---

### 5.4 API Endpoints

All endpoints are prefixed with `/api`.

#### Teams

| Method | Path | Body / Params | Description |
|--------|------|---------------|-------------|
| `POST` | `/teams` | `{name, parentId?, teamTypeId?}` | Create team |
| `GET` | `/teams` | `?tree=true` | List teams (flat or hierarchical) |
| `GET` | `/teams/{id}` | — | Get team by ID |
| `PUT` | `/teams/{id}` | `{name, teamTypeId?}` | Update team |
| `DELETE` | `/teams/{id}` | — | Delete team |
| `POST` | `/teams/import` | `file` (CSV) | Bulk import |
| `GET` | `/teams/{teamId}/categories` | — | Get category allocations |
| `PUT` | `/teams/{teamId}/categories` | `{categories:[{name,pct}]}` | Replace category allocations |

#### Employees

| Method | Path | Body / Params | Description |
|--------|------|---------------|-------------|
| `POST` | `/employees` | `{firstName, lastName, email, type?}` | Create employee |
| `GET` | `/employees` | — | List all employees |
| `GET` | `/employees/{id}` | — | Get employee |
| `PUT` | `/employees/{id}` | `{firstName, lastName, email, type?}` | Update employee |
| `DELETE` | `/employees/{id}` | — | Delete employee |
| `POST` | `/employees/import` | `file` (CSV) | Bulk import with type history |
| `GET` | `/employees/{id}/costs/type-history` | — | List type history |
| `POST` | `/employees/{id}/costs/type-history` | `{type, effectiveFrom}` | Add type history |
| `PUT` | `/employees/{id}/costs/type-history/{hid}` | `{type, effectiveFrom}` | Update type history entry |
| `DELETE` | `/employees/{id}/costs/type-history/{hid}` | — | Delete type history entry |
| `GET` | `/employees/{id}/costs/grade-history` | — | List grade history |
| `POST` | `/employees/{id}/costs/grade-history` | `{gradeId, effectiveFrom}` | Add grade history |
| `PUT` | `/employees/{id}/costs/grade-history/{hid}` | — | Update grade history entry |
| `DELETE` | `/employees/{id}/costs/grade-history/{hid}` | — | Delete grade history entry |
| `GET` | `/employees/{id}/costs/cost-history` | — | List direct cost history |
| `POST` | `/employees/{id}/costs/cost-history` | `{dailyCost, effectiveFrom}` | Add cost history |
| `PUT` | `/employees/{id}/costs/cost-history/{hid}` | — | Update cost history entry |
| `DELETE` | `/employees/{id}/costs/cost-history/{hid}` | — | Delete cost history entry |

#### Assignments

| Method | Path | Body / Params | Description |
|--------|------|---------------|-------------|
| `POST` | `/assignments` | `{teamId, employeeId, allocationPct, roleType, roleWeight, startDate, endDate?}` | Create assignment |
| `PUT` | `/assignments/{id}` | `{teamId, allocationPct, roleType, roleWeight, startDate, endDate?}` | Update assignment |
| `PUT` | `/assignments/{id}/allocation` | `{allocationPct}` | Update allocation only |
| `PUT` | `/assignments/{id}/role` | `{roleType, roleWeight, effectiveFrom}` | Change role (creates history) |
| `PUT` | `/assignments/{id}/end` | `?endDate=YYYY-MM-DD` | End assignment |
| `DELETE` | `/assignments/{id}` | — | Delete assignment |
| `POST` | `/assignments/import` | `file` (CSV) | Bulk import |
| `GET` | `/assignments/team/{teamId}` | — | Assignments for a team |
| `GET` | `/assignments/employee/{empId}` | — | Assignments for an employee |
| `GET` | `/assignments/{id}/roles` | — | Role history for assignment |

#### Capacity

| Method | Path | Body / Params | Description |
|--------|------|---------------|-------------|
| `GET` | `/capacity/team/{teamId}` | `?month=yyyy-MM` | Monthly capacity |
| `GET` | `/capacity/team/{teamId}/remaining` | `?date=YYYY-MM-DD` | Remaining capacity from date |
| `GET` | `/capacity/team/{teamId}/rollup` | `?month=yyyy-MM` | Rollup across sub-teams |
| `POST` | `/capacity/team/{teamId}/simulate` | `{month, changes:[{type,employeeId,...}]}` | What-if simulation |

#### Configuration

| Method | Path | Description |
|--------|------|-------------|
| `CRUD` | `/grades` | Grade management |
| `GET/POST` | `/grades/{id}/cost-history` | Grade cost history |
| `PUT/DELETE` | `/grades/{id}/cost-history/{hid}` | Edit/delete cost history entry |
| `CRUD` | `/role-types` | Role type management |
| `CRUD` | `/team-types` | Team type management |
| `CRUD` | `/public-holidays` | Holiday management |
| `GET/POST` | `/working-days` | Working days import and query |
| `CRUD` | `/alerts` | Capacity alert management |
| `GET/POST` | `/snapshots` | Snapshot generation and query |
| `GET` | `/audit` | Audit log query |

---

### 5.5 Sequence Diagrams

#### SD-1: Create Assignment

```
Manager (Browser)         Angular Frontend        Spring Controller       AssignmentService       DB
       │                        │                        │                       │                │
       │  Fill form & submit    │                        │                       │                │
       │───────────────────────>│                        │                       │                │
       │                        │  POST /api/assignments │                       │                │
       │                        │───────────────────────>│                       │                │
       │                        │                        │  assign(cmd)          │                │
       │                        │                        │──────────────────────>│                │
       │                        │                        │                       │ findTeam        │
       │                        │                        │                       │────────────────>│
       │                        │                        │                       │ findEmployee    │
       │                        │                        │                       │────────────────>│
       │                        │                        │                       │ save assignment │
       │                        │                        │                       │────────────────>│
       │                        │                        │                       │ save roleHistory│
       │                        │                        │                       │────────────────>│
       │                        │                        │                       │ save auditLog   │
       │                        │                        │                       │────────────────>│
       │                        │                        │  AssignmentResponse   │                │
       │                        │<───────────────────────│                       │                │
       │  Success toast shown   │                        │                       │                │
       │<───────────────────────│                        │                       │                │
```

#### SD-2: Compute Monthly Capacity

```
Manager (Browser)        Angular Frontend        Spring Controller       CapacityService        DB
       │                       │                        │                       │               │
       │  Select team & month  │                        │                       │               │
       │──────────────────────>│                        │                       │               │
       │                       │  GET /capacity/team/X  │                       │               │
       │                       │  ?month=yyyy-MM        │                       │               │
       │                       │───────────────────────>│                       │               │
       │                       │                        │  computeCapacity(q)   │               │
       │                       │                        │──────────────────────>│               │
       │                       │                        │                       │ load team      │
       │                       │                        │                       │───────────────>│
       │                       │                        │                       │ load assignments│
       │                       │                        │                       │  (month range) │
       │                       │                        │                       │───────────────>│
       │                       │                        │                       │ load roleHistory│
       │                       │                        │                       │───────────────>│
       │                       │                        │                       │ load categories │
       │                       │                        │                       │───────────────>│
       │                       │                        │                       │ load holidays   │
       │                       │                        │                       │───────────────>│
       │                       │                        │                       │ load workingDays│
       │                       │                        │                       │───────────────>│
       │                       │                        │                       │                │
       │                       │                        │                       │ CapacityCalculator.compute(...)
       │                       │                        │                       │ (pure domain, no I/O)
       │                       │                        │                       │
       │                       │                        │  CapacityResult       │
       │                       │<───────────────────────│                       │               │
       │  Breakdown displayed  │                        │                       │               │
       │<──────────────────────│                        │                       │               │
```

#### SD-3: Change Employee Role Mid-Assignment

```
Manager (Browser)        Angular Frontend        Spring Controller       AssignmentService       DB
       │                       │                        │                       │               │
       │  Enter new role &     │                        │                       │               │
       │  effectiveFrom date   │                        │                       │               │
       │──────────────────────>│                        │                       │               │
       │                       │  PUT /assignments/X/role                       │               │
       │                       │───────────────────────>│                       │               │
       │                       │                        │  changeRole(cmd)      │               │
       │                       │                        │──────────────────────>│               │
       │                       │                        │                       │ load assignment│
       │                       │                        │                       │───────────────>│
       │                       │                        │                       │ closeCurrentRole
       │                       │                        │                       │ (set effectiveTo)
       │                       │                        │                       │───────────────>│
       │                       │                        │                       │ save new       │
       │                       │                        │                       │ RoleHistory    │
       │                       │                        │                       │───────────────>│
       │                       │                        │  RoleHistoryResponse  │               │
       │                       │<───────────────────────│                       │               │
       │  Role history updated │                        │                       │               │
       │<──────────────────────│                        │                       │               │
```

#### SD-4: Bulk Import Employees from CSV

```
Manager (Browser)        Angular Frontend        Spring Controller       EmployeeService         DB
       │                       │                        │                       │               │
       │  Drag-drop CSV file   │                        │                       │               │
       │──────────────────────>│                        │                       │               │
       │                       │  POST /employees/import│                       │               │
       │                       │  (multipart)           │                       │               │
       │                       │───────────────────────>│                       │               │
       │                       │                        │  parseCsv(file)       │               │
       │                       │                        │  (extract rows)       │               │
       │                       │                        │                       │               │
       │                       │                        │  importBatch(cmds)    │               │
       │                       │                        │──────────────────────>│               │
       │                       │                        │                       │ for each row:  │
       │                       │                        │                       │  save employee │
       │                       │                        │                       │───────────────>│
       │                       │                        │                       │  (skip if dup) │
       │                       │                        │                       │               │
       │                       │                        │  for each imported:   │               │
       │                       │                        │  addTypeHistory(cmd)  │               │
       │                       │                        │──────────────────────>│               │
       │                       │                        │                       │ save type hist │
       │                       │                        │                       │───────────────>│
       │                       │                        │                       │               │
       │                       │                        │  ImportResponse       │               │
       │                       │<───────────────────────│ {imported,skipped,    │               │
       │                       │                        │  errors[]}            │               │
       │  Results shown inline │                        │                       │               │
       │<──────────────────────│                        │                       │               │
```

#### SD-5: Generate Capacity Snapshot for All Teams

```
Manager (Browser)        Angular Frontend        Spring Controller       SnapshotService        DB
       │                       │                        │                       │               │
       │  Click "Generate All" │                        │                       │               │
       │──────────────────────>│                        │                       │               │
       │                       │  POST /snapshots/all   │                       │               │
       │                       │  ?month=yyyy-MM        │                       │               │
       │                       │───────────────────────>│                       │               │
       │                       │                        │  generateAll(month)   │               │
       │                       │                        │──────────────────────>│               │
       │                       │                        │                       │ load all teams │
       │                       │                        │                       │───────────────>│
       │                       │                        │                       │               │
       │                       │                        │                       │ for each team: │
       │                       │                        │                       │  computeCapacity
       │                       │                        │                       │  deleteOldSnap │
       │                       │                        │                       │───────────────>│
       │                       │                        │                       │  saveAll(snaps)│
       │                       │                        │                       │───────────────>│
       │                       │                        │                       │  (skip team on │
       │                       │                        │                       │   error, log)  │
       │                       │                        │                       │               │
       │                       │                        │  204 No Content       │               │
       │                       │<───────────────────────│                       │               │
       │  Success notification │                        │                       │               │
       │<──────────────────────│                        │                       │               │
```

---

### 5.6 Infrastructure & Deployment

#### Docker Compose Services

| Service | Image | Port | Purpose |
|---------|-------|------|---------|
| `postgres` | `postgres:17` | 5432 | Primary data store |
| `adminer` | `adminer:4` | 8888 -> 8080 | Web-based DB browser |

The PostgreSQL container uses a named volume (`postgres_data`) for data persistence across restarts.

A health check (`pg_isready`) ensures Adminer only starts after the database is ready.

#### Running Locally

```bash
# Start database
docker-compose up -d

# Start backend (requires Java 21)
cd backend
./mvnw spring-boot:run

# Start frontend (requires Node 20+)
cd frontend
npm install
ng serve
```

| Service | URL |
|---------|-----|
| Angular SPA | http://localhost:4200 |
| Spring Boot API | http://localhost:8080 |
| Adminer | http://localhost:8888 |

#### Database Migrations

Liquibase runs automatically on backend startup. The single changelog file is:

```
backend/src/main/resources/db/changelog/db.changelog-master.sql
```

Changeset: `optimus:001-initial-schema` — creates all tables in their final state with no seed data.

#### CORS

The backend allows cross-origin requests from `http://localhost:4200` (configured in `application.yml`). In production, this should be updated to the deployed frontend origin.

#### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_USERNAME` | `postgres` | PostgreSQL username |
| `DB_PASSWORD` | `postgres` | PostgreSQL password |
| `server.port` | `8080` | Backend HTTP port |
