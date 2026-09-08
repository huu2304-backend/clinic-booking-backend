# Database Design — CBS

Schema reference, kept in sync with `db/migration/`. Update this file *before* changing schema in code.

**Note on migrate up/down (CBS-56):** this project uses Flyway Community (free), which has no automatic undo/rollback (`U__` undo scripts are Flyway Teams only). "Down" locally means `docker compose down -v` (drop the Postgres volume) then `docker compose up -d postgres` + rerun the app so Flyway replays every migration from scratch on an empty DB — there's no automatic step-back mechanism.

## Architecture decision
Patient/Doctor/Admin all authenticate via **one shared `account` table** (role column), with role-specific data in `*_profile` tables (1:1 FK). Rationale: `UserDetailsService` queries a single table regardless of role. Changing this later means revisiting the whole Security layer (UserDetailsService, JWT filter), not just migrations.

## Relationships (implemented)
```
account (1) ── (1) patient_profile
account (1) ── (1) doctor_profile ── (N:1) department
doctor_profile (1) ── (N) doctor_schedule
doctor_schedule (1) ── (0..1) appointment   [UNIQUE]
account [patient] (1) ── (N) appointment
```

## `account` (V1)
| Column | Type | Notes |
|---|---|---|
| id | BIGSERIAL PK | |
| email | VARCHAR(255) UNIQUE NOT NULL | login |
| password_hash | VARCHAR(255) NOT NULL | BCrypt, never returned by API |
| role | VARCHAR(20) NOT NULL, CHECK IN (PATIENT, DOCTOR, ADMIN) | |
| status | VARCHAR(20) NOT NULL DEFAULT ACTIVE, CHECK IN (ACTIVE, INACTIVE, LOCKED) | |
| created_at / updated_at | TIMESTAMP | |

## `patient_profile` (V2, `address` added in V9)
id · account_id (FK, UNIQUE) · full_name · date_of_birth · gender · phone_number · address. No medical data here.

## `department` (V3)
id · name (UNIQUE) · description

## `doctor_profile` (V4)
id · account_id (FK, UNIQUE — created by Admin, no self-registration) · full_name · department_id (FK)

## `doctor_schedule` (V6 + V7)
| Column | Type | Notes |
|---|---|---|
| id | BIGSERIAL PK | |
| doctor_profile_id | FK NOT NULL | |
| work_date / start_time / end_time | DATE/TIME | |
| status | VARCHAR(20) DEFAULT AVAILABLE, CHECK IN (AVAILABLE, LOCKED, BOOKED, CANCELLED) | `LOCKED` added in V7 |
| locked_by_account_id | FK → account, NULLABLE | who currently holds it |
| lock_expires_at | TIMESTAMP NULLABLE | hold TTL |
| version | BIGINT DEFAULT 0 | optimistic locking |

Unique: `(doctor_profile_id, work_date, start_time)`. Index: `(status, lock_expires_at)` for the expired-lock sweep job.

## `appointment` (V8)
id · doctor_schedule_id (FK **UNIQUE** — DB-level anti-double-booking) · patient_account_id (FK) · status (CONFIRMED/CANCELLED, default CONFIRMED) · created_at · cancelled_at

Service-layer rule (not expressible as a DB constraint): a patient can't have two CONFIRMED appointments with overlapping time — checked in `BookingServiceImpl.confirm()` before insert.

## `audit_log` (V10 — V9 intentionally skipped, already claimed by the still-open CBS-66 PR; Flyway tolerates gaps)
id · action (VARCHAR, CHECK IN `TRIAGE_SESSION_CREATED` / `APPOINTMENT_CREATED` / `APPOINTMENT_CANCELLED` / `SCHEDULE_CREATED` / `AI_PROVIDER_UPDATED`) · actor_account_id (FK, nullable — null when system-generated) · entity_type / entity_id (generic reference, **no hard FK** since each action points at a different table) · created_at

Append-only (BR-AIP-05): no `updated_at`/`deleted_at`, no service in the codebase is allowed to UPDATE/DELETE this table. No free-text column (no "details"/"payload") — deliberately, to make it structurally impossible to leak a password/token/raw secret/full symptom text into an audit row. New action values require a follow-up migration that drops and re-adds `chk_audit_log_action` (never edit V10).

**Planned, not yet implemented** (AI Triage, future migrations from V11 onward): `triage_session` (symptom input, suggested department, urgency, AI summary), `ai_execution` (per-call technical log, no raw content stored), `ai_provider_config` (Admin-only, secrets masked). `appointment` will later gain `triage_session_id` / `share_ai_summary_with_doctor` / `ai_summary_snapshot`.

## Access notes
Patient reads only their own account/profile/appointments. Doctor reads own profile + own appointments (AI summary only if the patient opted to share). Admin reads account metadata/department/doctor_profile — never medical content (triage/AI summary).

---
*Update this file when schema changes — don't let it drift from the real migrations.*
