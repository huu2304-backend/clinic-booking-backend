# CLAUDE.md

Spring Boot backend for **CBS (Clinic Booking System)** — appointment booking; core features are booking-conflict prevention (optimistic locking) and AI triage. This repo **is** the backend, flat at repo root — not a `backend/`+`frontend/` monorepo (README/DATABASE_DESIGN are kept in sync with code; if you ever see them drift, trust this file and fix them, don't guess).

## Stack
Java 17 · Maven (`mvnw`/`mvnw.cmd`) · Spring Boot 4.1.0 (Web MVC/JPA/Security/Validation) · PostgreSQL via Docker + **Flyway** (`ddl-auto=validate` — Flyway owns the schema, Hibernate never auto-generates tables) · JWT (jjwt 0.12.6) · springdoc-openapi (`/swagger-ui/index.html`) · MapStruct 1.6.3 (+ `lombok-mapstruct-binding`)

## Package layout (`src/main/java/com/clinicbookingbackend/`)
Feature-scoped by domain: each domain (`auth`, `account`, `department`, `doctor`, `doctorschedule`, `booking`, ...) has its own subpackage under `controller/`, `dto/`, `entity/`, `mapper/`, `repository/`, `service/`. `config`, `security`, `common/exception` are shared across the app. New domain → mirror the subpackage across layers, no need to update this file. `scheduler/` holds `@Scheduled` jobs, `seed/` holds startup `CommandLineRunner` demo-data seeding (`@Profile("!prod")` — never runs in production).
Note: `service/department` and `service/doctor` follow interface+Impl; `service/auth` and `service/booking` are concrete classes — both conventions coexist, not yet unified.

## Migrations
`src/main/resources/db/migration/V{n}__description.sql`, run in order, **never edit an applied file** (Flyway fails on checksum mismatch) — always add a new `V{n+1}`. Flyway tolerates gaps in the version sequence (doesn't need to be contiguous), which matters when multiple migration-adding branches are in flight at once off the same `develop` base — check open PRs for an already-claimed `V{n}` before picking a number. Current: V1 account, V2 patient_profile, V3 department, V4 doctor_profile, V5 seed (15 real departments + 50 **test** doctors sharing one password — not for real environments), V6 doctor_schedule (status `AVAILABLE/BOOKED/CANCELLED`), V7 adds lock columns to doctor_schedule (`locked_by_account_id`, `lock_expires_at`, `version`, status gains `LOCKED` — CBS-41), V8 creates `appointment` (UNIQUE `doctor_schedule_id` — CBS-41; no AI-related columns yet, planned for AI Triage Sprint 2), V9 adds `address` to `patient_profile` (CBS-66), V10 creates `audit_log` (append-only, CBS-56).

## Jira
Cloud site `huu2342003.atlassian.net`, project key `CBS`. Look up a task via Atlassian MCP `getJiraIssue(cloudId="huu2342003.atlassian.net", issueIdOrKey="CBS-xx")`. **Never guess a task's content from its branch name or number** — always check Jira before planning a specific task.

## Standard CRUD pattern for a new domain (from CBS-37/38, see `doctor`/`doctorschedule`)
- **Entity**: Lombok `@Getter/@Setter/@NoArgsConstructor/@AllArgsConstructor`, PK `@GeneratedValue(IDENTITY)`. No `@CreatedDate` framework usage anywhere — timestamps are set manually via `@PrePersist`/`@PreUpdate` when needed (see `Account.java`). `@Version` (optimistic locking) is used on `DoctorSchedule` (CBS-41/72/42) for the anti-double-booking flow — see `service/booking/BookingServiceImpl`; when `save()` loses the version race, Hibernate throws `ObjectOptimisticLockingFailureException`, handled in `GlobalExceptionHandler` as 409 `SLOT_UNAVAILABLE` (mirrors `DataIntegrityViolationException` handling).
- **DTO**: Create/Update request = Lombok `@Data` class (`jakarta.validation`), Response = a Java `record`.
- **Mapper**: MapStruct `@Mapper(componentModel="spring")`; mapper **must not call Repository** — resolve relation entities by id in the Service.
- **Repository**: `JpaRepository` + `existsBy...` to pre-check duplicates before insert/update (not just a DB constraint — the constraint is the last-resort safety net, `existsBy...` gives a clean business error).
- **Business errors**: `throw new BusinessException(ErrorCode.XXX)` — never build an error `ResponseEntity` by hand in a Service. Add new codes to `common/exception/ErrorCode.java`. `GlobalExceptionHandler` already maps `BusinessException`/`MethodArgumentNotValidException`/`DataIntegrityViolationException` → a standard `ApiError` response.
- **Authorization**: no `@PreAuthorize`/`@EnableMethodSecurity` yet — role-based access lives in path matchers in `config/SecurityConfig.java` (`.requestMatchers("/api/...").hasRole(...)`). The JWT principal in `SecurityContext` is a raw `Long accountId` (not `UserDetails`). For "ownership" checks (a user may only touch their own resource — not expressible via path matcher), use `security/SecurityUtils.getCurrentAccountId(authentication)` / `hasRole(authentication, "ROLE_NAME")` and check manually in the Service.
- **Tests**: Service tests use `@ExtendWith(MockitoExtension.class)` + `@InjectMocks` (no context load). Path-based authorization rules must use `@WebMvcTest(Controller.class)` + `@Import(SecurityConfig.class)` + `@MockitoBean JwtUtil` (**not** `addFilters=false`) — see `DoctorControllerSecurityTest.java`/`DoctorScheduleControllerSecurityTest.java`.
- **Migration**: new `V{n+1}` file, never edit an old one. Sync `docs/DATABASE_DESIGN.md` immediately when schema changes.

## Running locally
`.env.example` → `.env` (`DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_EXPIRATION`) → `docker compose up -d` (Postgres `cbs_postgres`) → `./mvnw spring-boot:run` (`mvnw.cmd` on Windows). Backend not yet Dockerized (planned Sprint 5). `DemoDataSeeder` (`seed/`) auto-creates demo accounts + doctor slots on every startup unless `SPRING_PROFILES_ACTIVE=prod` — see README "Demo accounts" for credentials.

## Git flow
`main` / `develop` / `feature/CBS-xx-description` (Jira key `CBS`). PRs merge into `develop`. Remote: `github.com/huu2304-backend/clinic-booking-backend`.

Keep this file in sync with the code whenever stack or structure changes.
