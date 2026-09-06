# CLAUDE.md

Backend Spring Boot của **CBS (Clinic Booking System)** — đặt lịch khám bệnh; cốt lõi là chống trùng lịch (optimistic locking) và AI triage. Repo này **là** backend, nằm phẳng ở root — không phải monorepo `backend/`+`frontend/` như README mô tả. File này là **sự thật về code**; README chỉ là bối cảnh sản phẩm và đã lệch ở vài điểm (xem "Bẫy README" bên dưới).

## Stack
Java 17 · Maven (`mvnw`/`mvnw.cmd`) · Spring Boot 4.1.0 (Web MVC/JPA/Security/Validation) · PostgreSQL qua Docker + **Flyway** (`ddl-auto=validate` — Flyway sở hữu schema, Hibernate không tự sinh bảng) · JWT (jjwt 0.12.6) · springdoc-openapi (`/swagger-ui/index.html`) · MapStruct 1.6.3 (+ `lombok-mapstruct-binding`)

## Package (`src/main/java/com/clinicbookingbackend/`)
Feature-scoped theo domain: mỗi domain (`auth`, `account`, `department`, `doctor`, ...) có subpackage riêng trong `controller/`, `dto/`, `entity/`, `mapper/`, `repository/`, `service/`. `config`, `security`, `common/exception` dùng chung toàn app. Domain mới → tạo subpackage cùng tên ở các layer trên, không cần sửa ghi chú này.
Lưu ý: `service/department` và `service/doctor` theo pattern interface + Impl; `service/auth` là class cụ thể — hai convention đang song song, chưa thống nhất.

## Migration
`src/main/resources/db/migration/V{n}__mo_ta.sql`, chạy tuần tự, **không sửa file đã áp dụng** (Flyway báo lỗi checksum) — luôn tạo `V{n+1}` mới. Hiện tại: V1 account, V2 patient_profile, V3 department, V4 doctor_profile, V5 seed (15 khoa thật + 50 doctor **TEST**, dùng chung 1 password — không dùng cho môi trường thật), V6 doctor_schedule (chỉ status `AVAILABLE/BOOKED/CANCELLED` — cột lock-related (`locked_by_account_id`, `lock_expires_at`, `version`, status `LOCKED`) sẽ thêm ở CBS-41 bằng `V7`, không sửa V6).

## Jira
Cloud site: `huu2342003.atlassian.net`, project key `CBS`. Tra cứu 1 task: dùng Atlassian MCP `getJiraIssue(cloudId="huu2342003.atlassian.net", issueIdOrKey="CBS-xx")`. **Không suy đoán nội dung task từ tên nhánh/số thứ tự** — luôn tra Jira trước khi lập plan cho 1 task cụ thể.

## Pattern CRUD chuẩn khi thêm domain mới (đúc kết từ CBS-37/38, tham chiếu domain `doctor`/`doctorschedule`)
- **Entity**: Lombok `@Getter/@Setter/@NoArgsConstructor/@AllArgsConstructor`, PK `@GeneratedValue(IDENTITY)`. Chưa dùng `@Version`/optimistic locking hay `@CreatedDate` framework ở đâu — timestamp thủ công qua `@PrePersist`/`@PreUpdate` nếu cần (xem `Account.java`).
- **DTO**: Create/Update request là Lombok `@Data` class (validate bằng `jakarta.validation`), Response là Java `record`.
- **Mapper**: MapStruct `@Mapper(componentModel="spring")`; mapper **không được gọi Repository** — set relation entity (fetch theo id) trong Service.
- **Repository**: `JpaRepository` + `existsBy...` để pre-check trùng trước khi insert/update (không chỉ dựa DB constraint — DB constraint là lưới an toàn cuối, `existsBy...` là để trả lỗi nghiệp vụ rõ nghĩa).
- **Lỗi nghiệp vụ**: `throw new BusinessException(ErrorCode.XXX)` — không tự dựng `ResponseEntity` lỗi trong Service. Thêm mã lỗi mới vào `common/exception/ErrorCode.java`. `GlobalExceptionHandler` đã tự map `BusinessException`/`MethodArgumentNotValidException`/`DataIntegrityViolationException` → response chuẩn (`ApiError`).
- **Authorization**: repo **chưa có** `@PreAuthorize`/`@EnableMethodSecurity` — mọi phân quyền theo ROLE nằm ở path-matcher trong `config/SecurityConfig.java` (`.requestMatchers("/api/...").hasRole(...)`). JWT principal trong `SecurityContext` là `Long accountId` thô (không phải `UserDetails`). Khi cần check "ownership" (user chỉ được đụng resource của chính mình — path-matcher không biểu đạt được luật này), dùng `security/SecurityUtils.getCurrentAccountId(authentication)`/`hasRole(authentication, "ROLE_NAME")` và tự check trong Service.
- **Test**: Service dùng `@ExtendWith(MockitoExtension.class)` + `@InjectMocks` (không load context). Riêng test rule phân quyền theo path phải dùng `@WebMvcTest(Controller.class)` + `@Import(SecurityConfig.class)` + `@MockitoBean JwtUtil` (**không** `addFilters=false`) — xem `DoctorControllerSecurityTest.java`/`DoctorScheduleControllerSecurityTest.java`.
- **Migration**: file mới `V{n+1}`, không sửa file cũ. Đồng bộ ngay `docs/DATABASE_DESIGN.md` khi đổi schema.

## Chạy local
`.env.example` → `.env` (`DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_EXPIRATION`) → `docker compose up -d` (Postgres `cbs_postgres`) → `./mvnw spring-boot:run` (`mvnw.cmd` trên Windows). Chưa Dockerize backend (dự kiến Sprint 5).

## Git flow
`main` / `develop` / `feature/CBS-xx-mo-ta` (Jira key `CBS`). PR merge vào `develop`. Remote: `github.com/huu2304-backend/clinic-booking-backend`.

## Bẫy README (đã lệch so với code thật)
- Tech stack: README ghi Java 21/Spring Boot 3.1.x — **sai**, thật là Java 17/Boot 4.1.0.
- Cấu trúc: README mô tả monorepo `backend/`+`frontend/` — **sai**, repo này chỉ có backend, phẳng ở root.
- Schema: README liệt kê đủ appointment/doctor_schedule/triage_session/ai_*/audit_log — **chưa implement đầy đủ**: đã có account/patient_profile/department/doctor_profile/doctor_schedule (bản CRUD cơ bản, chưa có cột lock-related — xem mục Migration); appointment/triage_session/ai_*/audit_log **vẫn chưa có**.

Khi stack/cấu trúc đổi → cập nhật file này ngay, đừng để nó lệch giống README.
