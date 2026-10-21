# Mini HRMS

A lightweight internal HR system: employees, departments, leave requests with manager approval, role-based access, a tamper-proof audit trail, and bulk CSV import.

**Stack:** React (Vite) · Spring Boot 3 (Java 21) · PostgreSQL 16 · Flyway · JWT auth · Docker

## Features

- **Three roles:** Admin, Manager and Employee, enforced on the server for every request
- **Leave workflow:** apply, then the manager approves or rejects, and the balance updates. Rules prevent overlapping dates, over-booking, approving your own leave, and deciding twice
- **Employee management:** add, edit, and deactivate (soft delete, so history is kept)
- **CSV import:** all-or-nothing, with row-by-row error reporting
- **Audit log:** every important change is recorded with who, what and when, in the same transaction as the change
- **Resilience:**
  - consistent JSON errors
  - database-level integrity rules
  - optimistic locking against conflicting edits
  - deactivated users are blocked immediately, even with a valid token

**Live demo:** _add your Vercel link here_ · To deploy your own copy for free, see **[DEPLOY.md](DEPLOY.md)**.

## Run it locally (one command)

1. Install **Docker Desktop** (https://www.docker.com/products/docker-desktop) and start it.
2. Open a terminal in this folder (the one containing `docker-compose.yml`) and run:

   ```
   docker compose up --build
   ```

   The first run downloads and builds everything (5–10 minutes). Later runs take seconds.
3. Open **http://localhost:3000**

| Demo login | Role | Password |
|---|---|---|
| admin@demo.com | Admin | password123 |
| manager@demo.com | Manager | password123 |
| priya@demo.com | Employee (reports to the manager) | password123 |
| arjun@demo.com | Employee (reports to the manager) | password123 |

- Stop: press `Ctrl + C`, or run `docker compose down`.
- Reset all data to the demo state: `docker compose down -v`, then `docker compose up --build`.

## Try this demo flow (2 minutes)

1. Log in as **priya@demo.com** and apply for leave.
2. Log out, then log in as **manager@demo.com**. Open **Approvals** and approve it.
3. Log in as **admin@demo.com**:
   - Open **Audit log** to see both actions recorded.
   - Open **Employees** and import `sample-employees.csv`. Then try `sample-employees-with-errors.csv` to see row-level validation.

## Project structure

```
backend/
  src/main/java/com/minihrms/
    config/       SecurityConfig (JWT, roles), DemoDataSeeder
    security/     TokenService (creates JWTs), CurrentUserService (who's calling)
    controller/   REST endpoints (thin)
    service/      business rules (leave workflow, CSV import, audit)
    repository/   database access (Spring Data JPA)
    model/        entities = tables
    dto/          request/response shapes (never expose entities)
    exception/    ApiException + GlobalExceptionHandler
  src/main/resources/db/migration/   Flyway SQL migrations (V1, V2, V3)
  src/test/       unit tests for the leave rules
frontend/
  src/api.js      single API client (token, errors)
  src/App.jsx     layout + role-based tabs
  src/pages/      Login, MyLeaves, Approvals, Employees, AuditLog, Profile
docker-compose.yml
```

## API summary

| Method | Path | Who |
|---|---|---|
| POST | /api/auth/login | anyone |
| GET | /api/me | logged in |
| POST | /api/me/password | logged in |
| GET | /api/departments | logged in |
| GET | /api/leaves/mine | logged in |
| POST | /api/leaves | logged in |
| GET | /api/leaves/pending | Manager, Admin |
| POST | /api/leaves/{id}/decision | Manager (own team), Admin |
| GET | /api/employees | Manager (team), Admin (all) |
| POST / PUT | /api/employees, /api/employees/{id} | Admin |
| PATCH | /api/employees/{id}/active | Admin |
| POST | /api/employees/import | Admin |
| GET | /api/admin/audit-logs | Admin |

## Build vs buy

Off-the-shelf HRMS tools (Zoho People, Frappe HR, Keka) are the right choice when a company needs payroll, compliance and statutory reports. This project covers the narrow core that a small team often wants to own and customise: leave rules, approvals, and an audit trail that integrates with its other internal tools. **Payroll would be bought, not built.**

## Known limitations / next steps

- The role inside a token is fixed until it expires (8h). Business checks reload the user from the database, but URL-level role checks use the token.
- No rate limiting on login yet.
- CSV parsing is simple: values cannot contain commas.
- Leave days count calendar days. Weekends and holidays are not excluded yet.
- No email notifications yet.

## Development without Docker

Run a local PostgreSQL (database, user and password all `hrms`), then:

```
cd backend && mvn spring-boot:run        # API on :8080
cd frontend && npm install && npm run dev # UI on :5173 (proxies /api to :8080)
cd backend && mvn test                    # unit tests
```

Built with AI-assisted development (Claude), with every rule reviewed and covered by tests.
