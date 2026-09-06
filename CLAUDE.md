# CLAUDE.md

Backend Spring Boot của **CBS (Clinic Booking System)** — đặt lịch khám bệnh; cốt lõi là chống trùng lịch (optimistic locking) và AI triage. Repo này **là** backend, nằm phẳng ở root — không phải monorepo `backend/`+`frontend/` như README mô tả. File này là **sự thật về code**; README chỉ là bối cảnh sản phẩm và đã lệch ở vài điểm (xem "Bẫy README" bên dưới).

## Stack
Java 17 · Maven (`mvnw`/`mvnw.cmd`) · Spring Boot 4.1.0 (Web MVC/JPA/Security/Validation) · PostgreSQL qua Docker + **Flyway** (`ddl-auto=validate` — Flyway sở hữu schema, Hibernate không tự sinh bảng) · JWT (jjwt 0.12.6) · springdoc-openapi (`/swagger-ui/index.html`) · MapStruct 1.6.3 (+ `lombok-mapstruct-binding`)

## Package (`src/main/java/com/clinicbookingbackend/`)
Feature-scoped theo domain: mỗi domain (`auth`, `account`, `department`, `doctor`, ...) có subpackage riêng trong `controller/`, `dto/`, `entity/`, `mapper/`, `repository/`, `service/`. `config`, `security`, `common/exception` dùng chung toàn app. Domain mới → tạo subpackage cùng tên ở các layer trên, không cần sửa ghi chú này.
Lưu ý: `service/department` và `service/doctor` theo pattern interface + Impl; `service/auth` là class cụ thể — hai convention đang song song, chưa thống nhất.

## Migration
`src/main/resources/db/migration/V{n}__mo_ta.sql`, chạy tuần tự, **không sửa file đã áp dụng** (Flyway báo lỗi checksum) — luôn tạo `V{n+1}` mới. Hiện tại: V1 account, V2 patient_profile, V3 department, V4 doctor_profile, V5 seed (15 khoa thật + 50 doctor **TEST**, dùng chung 1 password — không dùng cho môi trường thật).

## Chạy local
`.env.example` → `.env` (`DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_EXPIRATION`) → `docker compose up -d` (Postgres `cbs_postgres`) → `./mvnw spring-boot:run` (`mvnw.cmd` trên Windows). Chưa Dockerize backend (dự kiến Sprint 5).

## Git flow
`main` / `develop` / `feature/CBS-xx-mo-ta` (Jira key `CBS`). PR merge vào `develop`. Remote: `github.com/huu2304-backend/clinic-booking-backend`.

## Bẫy README (đã lệch so với code thật)
- Tech stack: README ghi Java 21/Spring Boot 3.1.x — **sai**, thật là Java 17/Boot 4.1.0.
- Cấu trúc: README mô tả monorepo `backend/`+`frontend/` — **sai**, repo này chỉ có backend, phẳng ở root.
- Schema: README liệt kê đủ appointment/doctor_schedule/triage_session/ai_*/audit_log — **chưa implement**, mới có account/patient_profile/department/doctor_profile.

Khi stack/cấu trúc đổi → cập nhật file này ngay, đừng để nó lệch giống README.
