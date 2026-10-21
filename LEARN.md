# Mini HRMS: Learning Guide

Read this with the code open in VS Code. Every section points to real files. By the end you should be able to explain every part of this project in an interview, without notes.

---

## 1. The big picture

```
 Browser (React)          nginx              Spring Boot API               PostgreSQL
 ───────────────        ─────────          ─────────────────────          ───────────
 Login, MyLeaves,  ──►  serves the    ──►  Controller  (HTTP in/out)
 Approvals, ...         React files,       Service     (business rules)  ──►  tables:
                        forwards /api      Repository  (DB access)       ◄──  employees,
 api.js adds the        to backend         + Security  (JWT, roles)           leave_requests,
 login token                               + Audit     (history)              audit_logs ...
```

**Explain it in one sentence:** *"It's a three-tier app: a React frontend, a Spring Boot REST API that holds all business rules and security, and PostgreSQL, with Flyway managing the schema and everything packaged with Docker Compose."*

---

## 2. Follow one request end to end

### A. Logging in

1. `frontend/src/pages/Login.jsx`: `submit()` calls `api('/auth/login', {POST, email, password})`.
2. `frontend/src/api.js`: sends `fetch('/api/auth/login')`. nginx forwards it to the backend.
3. `SecurityConfig.java`: `/api/auth/login` is `permitAll()`, so no token is needed.
4. `AuthController.login()`: `@Valid` checks that the email format is valid and the password isn't blank. Then it calls the service.
5. `AuthService.login()`:
   - finds the employee by email;
   - checks they're active;
   - checks the password with `passwordEncoder.matches(typed, storedHash)`.
   - If anything is wrong, it returns the **same** message, "Invalid email or password", so attackers can't find out which emails exist.
6. `TokenService.createToken()`: builds a **JWT** with `sub` = employee id, `role`, and `exp` = 8 hours from now. It's signed with the secret key.
7. The response `{ token, user }` comes back. `App.jsx` stores the token (`setToken`) and shows the tabs for that role.

### B. Manager approves a leave

1. `Approvals.jsx`: `decide(leave, true)` sends `POST /api/leaves/5/decision {approve:true}`.
2. `api.js` adds the header `Authorization: Bearer <token>`.
3. Spring Security (**authentication**) verifies the token's signature and expiry. If either is bad, it returns **401**.
4. `@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")` on `LeaveController.decide()` (**authorization**, coarse level): an employee would get **403** here.
5. `LeaveService.decide()` (**business rules**, fine level):
   - `currentUser.get()` reloads the manager **from the database**, so a deactivated user is blocked.
   - Rule 1: you can't decide your own leave.
   - Rule 2: a manager can only decide for their own team; an admin can decide for anyone.
   - Rule 3: the request must still be PENDING (otherwise **409 Conflict**).
   - Rule 4: re-check the balance, then deduct the days.
   - Set `status`, `decidedBy`, `decidedAt`.
   - `auditService.log(...)` writes an audit entry **in the same transaction**.
6. When the method ends, `@Transactional` commits. Hibernate sees the changed objects ("dirty checking") and writes UPDATEs. The `@Version` check makes sure nobody else changed these rows in the meantime.
7. A JSON `LeaveDto` goes back, and React shows "Approved".

If you can tell these two stories clearly, you understand 80% of the project.

---

## 3. Backend, file by file

| File | What it does | What to say in an interview |
|---|---|---|
| `pom.xml` | Dependencies: web, JPA, validation, security, JWT, Postgres, Flyway | "Spring Boot starters give compatible library versions." |
| `application.yml` | Settings. `${DB_URL:default}` reads environment variables | "No secrets in code. Production values come from the environment." |
| `db/migration/V1..V3` | SQL that creates and changes tables, run once each by Flyway | "The schema is versioned like code. I never edit an applied migration; I add a new one." |
| `model/*.java` | Entities: Java classes mapped to tables | "`@ManyToOne` maps foreign keys. Enums are stored as strings. `@Version` gives optimistic locking." |
| `repository/*.java` | Interfaces; Spring generates the queries | "Derived queries from method names, plus one JPQL `@Query` for date overlap." |
| `dto/*.java` | Records defining exact JSON in and out | "Entities never leave the server, so `password_hash` can't leak, and the API shape is stable." |
| `controller/*.java` | Thin HTTP layer + `@PreAuthorize` | "Controllers only translate HTTP. Rules live in services." |
| `service/*.java` | All business rules, `@Transactional` | "One transaction per use case: everything succeeds or everything rolls back." |
| `security/TokenService` | Creates signed JWTs | "HS256 signature, 8h expiry, id and role as claims." |
| `security/CurrentUserService` | "Who is calling?" Reloads from the DB | "A valid token isn't enough: deactivated users are blocked immediately." |
| `config/SecurityConfig` | Security rules, BCrypt, JWT encoder/decoder | "Stateless; CSRF is off because the token is in a header, not a cookie." |
| `config/DemoDataSeeder` | Creates demo users on the first start | "Passwords are hashed in Java, never stored as plain text in SQL." |
| `exception/GlobalExceptionHandler` | Turns every error into consistent JSON | "Graceful failure: users see a clear message, logs get the details." |

### Why the layers (Controller → Service → Repository)?
Each layer has one job. You can test the leave rules (`LeaveServiceTest`) without a web server or a database. If you swapped Postgres for something else, only the repositories would change.

---

## 4. Security, in depth

**Authentication vs authorization.** Authentication answers "who are you?" (the token is checked on every request). Authorization answers "what are you allowed to do?" (roles and ownership rules).

**Three layers of authorization (defence in depth):**
1. **URL rule** in `SecurityConfig`: everything under `/api/admin/**` needs ADMIN.
2. **Method rule** with `@PreAuthorize` on controller methods.
3. **Data/ownership rule** in services: "a manager only approves *their own team*." A role alone can't express this, because it depends on the data.

**Passwords.** BCrypt is one-way, salted, and deliberately slow, which makes brute-forcing stolen hashes expensive. We never decrypt; we compare hashes with `matches()`.

**JWT.** The token has three parts, `header.payload.signature`. The payload is only Base64 encoded, so **anyone can read it, but nobody can change it** without the secret, because the signature would break. That's why we never put secrets in the payload.

**Why stateless?** The server stores no sessions, so you can run several backend copies behind a load balancer.

**Trade-off to mention yourself:** the role inside a token stays the same until the token expires. Services reload the user from the database, so deactivation takes effect immediately. A role change, though, only fully applies after the user logs in again. The fix would be shorter token lifetimes plus refresh tokens.

**The frontend hides tabs, but that isn't security.** Anyone can call the API with curl or Postman. The backend enforces everything.

---

## 5. Leave rules (`LeaveService`)

| Rule | Where | Error |
|---|---|---|
| End date ≥ start date | service **and** DB `CHECK` | 400 |
| Can't start in the past | service | 400 |
| Max 30 days per request | service | 400 |
| No overlap with pending or approved leave | `hasOverlap` JPQL query | 409 |
| Requested + already pending ≤ balance | service | 400 |
| Can't decide own leave | service | 403 |
| Manager decides only for own team | service | 403 |
| Decide only once (PENDING only) | service | 409 |
| Balance re-checked at approval | service; DB `CHECK (leave_balance >= 0)` | 409 |

**The overlap logic:** two ranges overlap when `existing.start <= new.end AND existing.end >= new.start`. Draw two lines on paper and convince yourself. Interviewers like this.

**Days:** `getDays()` counts calendar days including both ends (Mon to Wed = 3). Weekends aren't excluded; that's a good extension to build yourself (section 12).

---

## 6. Data integrity and resilience

1. **Database constraints:** `UNIQUE(email)`, `CHECK` on roles, statuses, dates and balance, and foreign keys. Even a bug can't save impossible data.
2. **Validation at the edge:** `@NotBlank`, `@Email` and `@Size` on DTOs give a 400 with field names.
3. **Transactions:** a leave approval, the balance deduction and the audit log commit together, or none of them do.
4. **Optimistic locking (`@Version`):** if two admins edit the same employee at once, the second save gets a 409 instead of silently overwriting the first.
5. **Soft delete:** employees are deactivated, never deleted, so leave history and audit logs stay valid.
6. **Consistent errors:** `GlobalExceptionHandler`. Real bugs get logged and the user sees a polite 500.
7. **Health check:** `GET /api/health`. Docker also waits for the database to be ready (`healthcheck`).

---

## 7. Audit log

- **What's logged:** LEAVE_APPLIED / APPROVED / REJECTED, EMPLOYEE_CREATED / UPDATED / ACTIVATED / DEACTIVATED, EMPLOYEES_IMPORTED, PASSWORD_CHANGED.
- **Each entry has:** who (`actor`), what (`action`, `entity`, `entityId`), `details` (for example "role: EMPLOYEE -> MANAGER"), and when.
- **It can't be changed:** the `AuditLog` entity has no setters, and no API edits or deletes logs.
- **It's in the same transaction** as the change, so you never get a change without its log, or a log for a change that failed.
- **It's paginated** (20 per page), so the screen stays fast even with a million rows.

---

## 8. CSV import (`CsvImportService`)

- The header must be `name,email,role,department,manager_email`.
- **All-or-nothing:** every row is validated first. If any row is bad, nothing is saved, and you get "Row 3: invalid email; role must be..."
- **Checks per row:** empty name, email format, duplicate in the file, email already in the DB, valid role, department exists, manager exists and is a MANAGER or ADMIN.
- It strips a UTF-8 BOM (`﻿`), an invisible character Excel adds.
- **Why all-or-nothing?** A half-imported file is hard to fix: which rows went in? Telling the admin exactly what to fix is better.
- **Limitation:** no quoted commas. A library like OpenCSV would fix that, which is a build-vs-buy decision in miniature.

Try it with `sample-employees.csv` and `sample-employees-with-errors.csv`.

---

## 9. Frontend

- `api.js`: **one place** for HTTP. It adds the token, turns errors into `Error(message)`, and on a 401 logs out everywhere using a browser event.
- `App.jsx`: holds `user` state and decides which tabs to show by role. On refresh it calls `/api/me` to restore the session from the saved token.
- **Pages:** each page loads its data in `useEffect`, keeps form state in `useState`, and shows success or error alerts.
- **CORS:** locally, the browser only talks to one address (nginx or Vite's proxy forwards `/api`), so CORS doesn't come up. Online, the frontend (Vercel) and backend (Render) are on **different addresses**. Browsers block that by default, so the backend lists allowed frontends in `corsConfigurationSource()` in `SecurityConfig`, read from `CORS_ALLOWED_ORIGINS`. The frontend finds the backend through `VITE_API_URL` in `api.js`.
- **Token storage:** `localStorage` is simple, but readable by any script on the page (an XSS risk). An httpOnly cookie is the more secure alternative, but then you need CSRF protection. Mentioning this trade-off shows maturity.

---

## 10. Docker and deployment

- `backend/Dockerfile`: **multi-stage**. Stage 1 builds the jar with Maven; stage 2 runs it on a small JRE-only image. No source code or build tools ship in the final image.
- `frontend/Dockerfile`: builds React to static files, then serves them with nginx.
- `docker-compose.yml`: three services. The backend waits for the DB healthcheck. The data volume keeps data between restarts.
- **To put it online:** Render or Railway can run the backend from the Dockerfile, with a managed Postgres (Neon or Render). You set `DB_URL`, `DB_USER`, `DB_PASSWORD` and a strong `JWT_SECRET` as environment variables. The frontend can go on Vercel or Netlify, or into the same nginx image.

---

## 11. Tests (`backend/src/test`)

- `LeaveServiceTest` uses **Mockito mocks** in place of the database, so it tests only the business rules and runs in milliseconds.
- **It covers:**
  - approval deducts the balance
  - rejection doesn't
  - can't approve your own leave
  - can't approve another team's leave
  - can't decide twice
  - can't exceed the balance
  - invalid dates are rejected
- Run them with `mvn test`, or the green ▶ next to the class in IntelliJ or VS Code.
- **Your answer to "How do you trust AI-generated code?":** "I review the diff, and I write tests for the rules that matter, like these. If the AI breaks a rule, a test fails."

---

## 12. Make it yours: do these before the interview

These are small, real changes. Doing them yourself (with Cursor or Claude Code) is what lets you honestly say you own this code.

1. **Exclude weekends** from `getDays()` in `LeaveRequest.java`, and add a test for it.
2. **Cancel a pending request:** add `CANCELLED` to `LeaveStatus`, a migration that updates the CHECK constraint, `POST /api/leaves/{id}/cancel` (owner only, PENDING only), an audit entry, and a button in `MyLeaves.jsx`.
3. **Add a `phone` column:** add `V4__add_phone.sql`, update the entity, DTO and form.
4. **Filter the audit log** by action, using a new repository method and a dropdown in `AuditLog.jsx`.
5. **Deploy it** to Render with Neon Postgres, and put the live link on your resume.

---

## 13. Interview questions: practise answering out loud

1. **Walk me through what happens when a manager clicks Approve.** → Section 2B.
2. **Authentication vs authorization?** → Section 4.
3. **How are passwords stored?** → BCrypt: hash plus salt, one-way, slow on purpose.
4. **What is a JWT and how do you prevent tampering?** → A signed token; changing it breaks the signature.
5. **Why can't a manager approve another team's leave? Where is that enforced?** → `LeaveService.decide`, on the server, using data, not just the role.
6. **What if two managers approve the same request at the same time?** → The status check plus `@Version`: the second one gets a 409.
7. **Why DTOs instead of returning entities?** → No sensitive fields leak, the API shape stays stable, and lazy-loading problems are avoided.
8. **Why Flyway instead of `ddl-auto=update`?** → Repeatable, versioned, reviewable schema changes, and identical on every environment.
9. **What does `@Transactional` do here?** → Approval, balance and audit commit together.
10. **How do you handle errors?** → `ApiException` + `GlobalExceptionHandler` give consistent JSON; real bugs are logged and the user sees a 500.
11. **Why soft delete?** → History and foreign keys stay valid.
12. **How would you scale this?** → The API is stateless, so add instances. Add DB indexes (already there), pagination (already there), and caching for departments.
13. **What would you improve?** → Refresh tokens, login rate limiting, weekend and holiday handling, email notifications, a proper CSV library, and integration tests with Testcontainers.
14. **Build vs buy: would you build payroll?** → No. It's compliance-heavy and not core, so you buy it. Leave approvals and audit are simple, and owning them lets us customise and integrate with our other tools.
15. **How did you use AI tools?** → "I used Claude to generate the structure and first drafts, then reviewed each layer, wrote and ran tests for the rules, fixed what didn't fit, and extended it myself (for example, weekend exclusion)." Only say the last part once you've actually done it.
16. **Why is the 'hide the tab' check not enough?** → Anyone can call the API directly; the server enforces everything.
17. **What's the N+1 problem? Does this app have it?** → Loading a list, then one extra query per row for lazy relations. Mapping DTOs that read `department` and `manager` can trigger it on large lists. The fix is `JOIN FETCH` or an `@EntityGraph`. Being honest about this impresses interviewers.

---

## 14. Glossary

- **REST API:** URLs plus HTTP methods (GET reads, POST creates, PUT replaces, PATCH changes part, DELETE removes) that exchange JSON.
- **Status codes:** 200 OK · 201 Created · 204 No Content · 400 bad input · 401 not logged in · 403 not allowed · 404 not found · 409 conflict · 500 server bug.
- **Entity:** a Java class mapped to a table. **DTO:** the shape of the API's JSON. **Repository:** DB access.
- **Dependency injection:** Spring creates objects and passes them into constructors; we never call `new Service()` ourselves.
- **Migration:** a versioned SQL change. **Transaction:** all-or-nothing database work.
- **Container:** a packaged app with everything it needs. **Image:** the template a container is started from.
