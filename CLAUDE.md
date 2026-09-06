# CLAUDE.md

Ghi chú định hướng cho Claude Code (và dev mới) khi làm việc trong repo này — đọc trước khi đụng vào code.

## Dự án này là gì

**CBS (Clinic Booking System)** — hệ thống đặt lịch khám bệnh, hai bài toán cốt lõi: chống trùng lịch hẹn (optimistic locking) và AI triage (LLM gợi ý khoa khám từ triệu chứng). Đang ở giai đoạn Sprint 1: auth, department, doctor profile. Đây **là** backend, nằm ở root repo — không phải monorepo có `backend/`/`frontend/` như README mô tả.

> `README.md` là tài liệu tổng quan cho người, viết bằng tiếng Việt, khá đầy đủ về business rules và roadmap — nhưng đã lệch so với code ở vài chỗ (xem mục "Bẫy dễ gặp" bên dưới). Coi `README.md` là bối cảnh sản phẩm, coi file này là sự thật về code.

## Tech stack thực tế

- **Java 17**, Maven (dùng `mvnw`/`mvnw.cmd`, không cần cài Maven riêng)
- **Spring Boot 4.1.0** — Web MVC, Data JPA, Security, Validation
- **PostgreSQL** qua Docker, migration bằng **Flyway** (`spring.jpa.hibernate.ddl-auto=validate` — schema do Flyway quản lý hoàn toàn, Hibernate không tự sinh bảng)
- **JWT** (jjwt 0.12.6) cho auth
- **springdoc-openapi** — Swagger UI tại `/swagger-ui/index.html` khi app chạy
- **MapStruct** (1.6.3) — mapping Entity ↔ DTO, dùng `lombok-mapstruct-binding` để tương thích với Lombok

## Cấu trúc package

`src/main/java/com/clinicbookingbackend/`
- `config` — Spring config (security, v.v.)
- `controller`, `controller/auth`, `controller/department`, `controller/doctor` — REST endpoints
- `dto`, `dto/auth`, `dto/department`, `dto/doctor` — request/response objects
- `entity`, `entity/account`, `entity/account/enums`, `entity/department`, `entity/doctor` — JPA entities
- `mapper`, `mapper/department`, `mapper/doctor` — MapStruct Entity ↔ DTO mapper
- `repository/account`, `repository/department`, `repository/doctor` — Spring Data JPA repositories (feature-scoped, không còn flat)
- `security` — JWT filter, auth-related security beans
- `service`, `service/auth`, `service/account`, `service/department`, `service/doctor` — business logic. `service/department` và `service/doctor` theo pattern interface + Impl; `service/auth` là class cụ thể (chưa thống nhất, xem lịch sử PR CBS-37)
- `common/exception` — exception handling dùng chung

## Database & migrations

Flyway migrations ở `src/main/resources/db/migration/`, đặt tên `V{n}__mo_ta.sql`, chạy tuần tự và không được sửa lại file đã áp dụng (Flyway sẽ báo lỗi checksum). Hiện có:
- `V1__create_account_table.sql`
- `V2__create_patient_profile_table.sql`
- `V3__create_department_table.sql`
- `V4__create_doctor_profile_table.sql`
- `V5__seed_department_and_doctor_data.sql` — seed 15 khoa thật + 50 tài khoản bác sĩ TEST (email/password giả, dùng chung 1 password) — xem comment đầu file, không dùng các tài khoản này cho môi trường thật

Muốn thêm bảng/cột mới → tạo migration mới với số thứ tự tiếp theo, không sửa migration cũ.

## Chạy dự án ở local

1. Copy `.env.example` → `.env`, điền `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_EXPIRATION` (`.env` đã bị gitignore, không commit).
2. `docker compose up -d` — chạy container Postgres (`cbs_postgres`, port `5432`, image `postgres:16-alpine`, data lưu trong volume `pgdata`).
3. `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`).

Chưa có Dockerfile cho backend — backend hiện chỉ chạy trực tiếp bằng Maven, không containerize (dự kiến ở Sprint 5 theo roadmap).

## Git flow

`main` / `develop` / `feature/CBS-xx-mo-ta` theo Jira (project key `CBS`). PR merge vào `develop`, review theo ticket. Remote: `github.com/huu2304-backend/clinic-booking-backend`.

## Bẫy dễ gặp (README vs code thật)

- README ghi Java 21 / Spring Boot 3.1.x ở badge và bảng tech stack — **sai**, `pom.xml` thực tế là Java 17 / Spring Boot 4.1.0.
- README mô tả cấu trúc thư mục dạng `backend/`, `frontend/` (monorepo) — **không khớp**, repo này chỉ là backend, nằm phẳng ở root.
- README liệt kê schema đầy đủ (appointment, doctor_schedule, triage_session, ai_execution, ai_provider_config, audit_log...) nhưng migration hiện tại mới có account/patient_profile/department/doctor_profile — phần còn lại **chưa được implement**, đừng giả định đã có sẵn.
- Khi cấu trúc/stack thay đổi, cập nhật lại file này — đừng để nó lệch giống README.
