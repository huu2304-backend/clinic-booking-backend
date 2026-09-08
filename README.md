# AI-Powered Clinic Booking System (CBS) — Backend

> Spring Boot REST API for a clinic appointment booking system. Core focus: booking-conflict prevention (optimistic locking + temporary slot hold) and AI-assisted patient triage (planned).

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-green)
![Status](https://img.shields.io/badge/status-in%20development-yellow)

This repo is the **backend only**, flat at root (not a `backend/`+`frontend/` monorepo). A separate frontend calls this API — see the CORS config in `SecurityConfig`.

## Roles
- **Patient**: register/login, view/update own profile, browse departments & doctors, view available slots, hold → confirm a booking, cancel, AI triage chat (planned)
- **Doctor**: view own daily appointments
- **Admin**: CRUD Department/Doctor

## Stack
Java 17 · Spring Boot 4.1.0 (Web MVC, JPA, Security, Validation) · PostgreSQL (Docker) · Flyway · JWT (jjwt) · springdoc-openapi · MapStruct · Maven

## Project structure
```
src/main/java/com/clinicbookingbackend/
├── controller/         REST controllers, per domain
├── service/             business logic, per domain
├── repository/           Spring Data JPA
├── entity/                 JPA entities
├── dto/                     request/response DTOs
├── mapper/                   MapStruct
├── security/                   JWT filter, SecurityUtils
├── scheduler/                    scheduled jobs (e.g. expired-lock sweep)
├── seed/                           demo data seeding (CommandLineRunner, disabled in prod)
└── common/exception/               BusinessException, ErrorCode, GlobalExceptionHandler
src/main/resources/db/migration/  Flyway V1..Vn
docs/DATABASE_DESIGN.md           schema reference
```

## Running locally
```bash
cp .env.example .env   # fill in DB_PASSWORD, JWT_SECRET
docker compose up -d   # starts Postgres (cbs_postgres)
./mvnw spring-boot:run # mvnw.cmd on Windows
```
API: `http://localhost:8080` · Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## Demo accounts
On every startup, `DemoDataSeeder` idempotently seeds demo data on top of the 15 departments / 50 test doctors already loaded by migration V5 — unless `SPRING_PROFILES_ACTIVE=prod`, in which case it does not run at all (no demo data is ever inserted into a production database).

| Role | Email | Password | Notes |
|---|---|---|---|
| Admin | `admin@cbs.local` | `Admin@123` | full Admin access |
| Patient | `benhnhan001@cbs.local` / `benhnhan002@cbs.local` | `Patient@123` | 2 ready-to-login patients |
| Doctor | `bacsi001@cbs.local` … `bacsi050@cbs.local` | `Doctor@123` | from migration V5, shared password — **test data only** |

For each department, the first 2 doctors (by id) get `AVAILABLE` slots seeded for the next 3 days (08:00–10:00, 30-minute increments) so a booking demo works immediately without manually creating schedules through Swagger.

## Environment variables (`.env`)
| Var | Default | Purpose |
|---|---|---|
| `DB_NAME` | `cbs_db` | Postgres database name |
| `DB_USERNAME` | `postgres` | Postgres user |
| `DB_PASSWORD` | — (required) | Postgres password |
| `JWT_SECRET` | — (required) | JWT signing key |
| `JWT_EXPIRATION` | `86400000` | Token TTL (ms) |
| `SLOT_LOCK_TTL_MINUTES` | `5` | How long a held slot stays `LOCKED` before auto-release |
| `LOCK_SWEEP_INTERVAL_MS` | `60000` | Interval of the expired-lock sweep job |
| `CANCELLATION_MIN_HOURS` | `24` | Minimum hours before an appointment's start time that a patient may still cancel it |
| `SPRING_PROFILES_ACTIVE` | unset | Set to `prod` to disable `DemoDataSeeder` (no demo data seeded) |

## Core business rules
- `doctor_schedule.status` ∈ `AVAILABLE / LOCKED / BOOKED / CANCELLED`; `LOCKED` has a TTL and auto-reverts via a scheduled sweep job
- Booking is two-step: `POST /api/schedules/{id}/hold` (AVAILABLE→LOCKED) then `POST /api/appointments` (LOCKED→BOOKED + creates `Appointment` CONFIRMED), both guarded by `@Version` optimistic locking
- DB-level safety net: `UNIQUE(appointment.doctor_schedule_id)`
- A patient can't hold/confirm a slot that's `BOOKED`, or `LOCKED` by someone else and not yet expired
- `POST /api/appointments/{id}/cancel`: only the owning patient can cancel their own `CONFIRMED` appointment, and only up to `CANCELLATION_MIN_HOURS` before its start time; cancelling sets `Appointment.status=CANCELLED` and reverts `doctor_schedule.status` to `AVAILABLE` in one transaction
- `GET`/`PUT /api/patients/me`: a patient views/edits their own `PatientProfile` (accountId from JWT, not a path/query param) — no medical data lives on this profile
- `GET /api/doctors/me/appointments?date=`: a doctor views their own `CONFIRMED` appointments for a given date — ownership via the DoctorProfile tied to the JWT accountId, doctorId is never accepted as a request parameter

## Implemented so far
Auth (register/login, JWT) · Department & Doctor CRUD (Admin) · Doctor schedule CRUD · Patient browse doctors/slots by department · Concurrency-safe booking (hold → confirm) · Expired-lock sweep job · Patient profile view/update · Demo data seeding (non-prod) · Appointment cancellation · Doctor daily appointment list

Not yet implemented: AI triage, AI provider config admin, containerized backend deploy. Full task breakdown lives in Jira (project **CBS**), not tracked here.

## Testing
JUnit 5 + Mockito for unit tests; `@SpringBootTest` for transactional/concurrency-critical flows against a real Postgres via Flyway (e.g. `BookingConcurrencyIntegrationTest`).

## Git flow
`main` / `develop` / `feature/CBS-xx-description`, PRs merge into `develop`.

---
*Keep this file in sync with the code when stack, structure, or implemented features change.*
