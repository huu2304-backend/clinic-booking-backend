# Database Design — AI-Powered Clinic Booking System

> File này mô tả schema DB ở mức thiết kế (trước khi viết Entity/Migration thật).
> Cập nhật file này TRƯỚC khi đổi schema trong code — đừng để lệch giữa thiết kế và migration thật.
> Nguồn nghiệp vụ gốc: `docs/business-rules.docx` (AI.docx đã phân tích ở phần PLAN).

---

## ✅ Quyết định kiến trúc (đã chốt 2026-08 — trước khi code CBS-35/36)

**Vấn đề:** Patient, Doctor, Admin đều cần đăng nhập (JWT), nhưng có attribute khác nhau.

**Quyết định:** Dùng **1 bảng `account` làm gốc xác thực dùng chung cho cả 3 role**, tách riêng field đặc thù từng role ra bảng `_profile` nối qua FK 1-1.

**Lý do chọn hướng này thay vì 3 bảng tách biệt:**
- Spring Security `UserDetailsService` chỉ cần implement **1 lần**, query đúng 1 bảng `account` bất kể role — nếu tách 3 bảng riêng, Login phải tự dò lần lượt 3 bảng, code phức tạp và chậm hơn không cần thiết.
- Giải quyết luôn câu hỏi "Admin đăng nhập bằng gì" chưa từng được định nghĩa rõ trước đây — Admin chỉ là 1 dòng trong `account` với `role = ADMIN`, không cần bảng riêng.
- Đúng khớp với README đã viết trước đó: `patient / patient_profile — Tài khoản & hồ sơ Patient`.

**Muốn đổi hướng sau này:** sửa lại xong phải quay lại review toàn bộ tầng Security (UserDetailsService, JWT filter) — không chỉ đổi migration.

---

## Sơ đồ quan hệ (mức tổng quan)

```
account (1) ──── (1) patient_profile
account (1) ──── (1) doctor_profile ──── (N:1) department
account (Patient) (1) ──── (N) triage_session
account (Patient) (1) ──── (N) appointment
doctor_profile (1) ──── (N) doctor_schedule
doctor_schedule (1) ──── (0..1) appointment      [UNIQUE constraint]
triage_session (0..1) ──── (0..1) appointment
triage_session (N) ──── (1) ai_execution
ai_execution (N) ──── (1) ai_provider_config
account (Admin, 1) ──── (N) ai_provider_config   [created_by]
account (N) ──── (N) audit_log                   [actor, không FK cứng]
```

---

## 1. `account` — Bảng gốc xác thực (dùng chung Patient/Doctor/Admin)

| Cột | Kiểu | Ràng buộc | Ghi chú |
|---|---|---|---|
| `id` | BIGSERIAL | PK | |
| `email` | VARCHAR(255) | UNIQUE, NOT NULL | Dùng để login |
| `password_hash` | VARCHAR(255) | NOT NULL | BCrypt, không bao giờ trả ra API |
| `role` | VARCHAR(20) | NOT NULL, CHECK IN (`PATIENT`, `DOCTOR`, `ADMIN`) | Dùng để phân quyền JWT |
| `status` | VARCHAR(20) | NOT NULL, DEFAULT `ACTIVE`, CHECK IN (`ACTIVE`, `INACTIVE`, `LOCKED`) | BR-ACC-04 — tài khoản bị khoá vẫn login được nhưng bị chặn ở tầng nào đó (cần làm rõ ở CBS-36) |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT now() | |
| `updated_at` | TIMESTAMP | NULLABLE | |

**Index cần có:** UNIQUE trên `email` (bắt buộc, phục vụ cả check trùng lẫn tốc độ query login).

---

## 2. `patient_profile` — Hồ sơ riêng của Patient

| Cột | Kiểu | Ràng buộc | Ghi chú |
|---|---|---|---|
| `id` | BIGSERIAL | PK | |
| `account_id` | BIGINT | FK → `account.id`, UNIQUE, NOT NULL | 1-1 với account |
| `full_name` | VARCHAR(255) | NOT NULL | |
| `date_of_birth` | DATE | NULLABLE | |
| `gender` | VARCHAR(10) | NULLABLE, CHECK IN (`MALE`, `FEMALE`, `OTHER`) | |
| `phone_number` | VARCHAR(20) | NULLABLE | |

**KHÔNG chứa dữ liệu y tế** — đúng BR-ACC-05, dữ liệu y tế nằm ở `triage_session`/`appointment`.

---

## 3. `department` — Khoa khám

| Cột | Kiểu | Ràng buộc | Ghi chú |
|---|---|---|---|
| `id` | BIGSERIAL | PK | |
| `name` | VARCHAR(100) | UNIQUE, NOT NULL | |
| `description` | TEXT | NULLABLE | |

---

## 4. `doctor_profile` — Hồ sơ riêng của Doctor

| Cột | Kiểu | Ràng buộc | Ghi chú |
|---|---|---|---|
| `id` | BIGSERIAL | PK | |
| `account_id` | BIGINT | FK → `account.id`, UNIQUE, NOT NULL | Tài khoản Doctor do Admin tạo (CBS-37), không tự đăng ký |
| `full_name` | VARCHAR(255) | NOT NULL | |
| `department_id` | BIGINT | FK → `department.id`, NOT NULL | |

---

## 5. `doctor_schedule` — Slot khám

| Cột | Kiểu | Ràng buộc | Ghi chú |
|---|---|---|---|
| `id` | BIGSERIAL | PK | |
| `doctor_profile_id` | BIGINT | FK → `doctor_profile.id`, NOT NULL | |
| `work_date` | DATE | NOT NULL | |
| `start_time` | TIME | NOT NULL | |
| `end_time` | TIME | NOT NULL | |
| `status` | VARCHAR(20) | NOT NULL, DEFAULT `AVAILABLE`, CHECK IN (`AVAILABLE`, `LOCKED`, `BOOKED`, `CANCELLED`) | BR-SCH-01 |
| `locked_by_account_id` | BIGINT | FK → `account.id`, NULLABLE | Patient đang giữ chỗ |
| `lock_expires_at` | TIMESTAMP | NULLABLE | BR-SCH-05, TTL giữ chỗ |
| `version` | BIGINT | NOT NULL, DEFAULT 0 | Optimistic Locking, BR-APT-02 |

**Index cần có:** composite index trên `(status, lock_expires_at)` — phục vụ job CBS-52 quét LOCKED hết hạn (đã note trong Jira CBS-41).

---

## 6. `appointment` — Lịch hẹn đã xác nhận

| Cột | Kiểu | Ràng buộc | Ghi chú |
|---|---|---|---|
| `id` | BIGSERIAL | PK | |
| `doctor_schedule_id` | BIGINT | FK → `doctor_schedule.id`, **UNIQUE**, NOT NULL | BR-APT-01 — chống trùng ở tầng DB |
| `patient_account_id` | BIGINT | FK → `account.id`, NOT NULL | |
| `status` | VARCHAR(20) | NOT NULL, DEFAULT `CONFIRMED`, CHECK IN (`CONFIRMED`, `CANCELLED`) | |
| `triage_session_id` | BIGINT | FK → `triage_session.id`, NULLABLE | Có thể đặt lịch không qua AI |
| `share_ai_summary_with_doctor` | BOOLEAN | NOT NULL, DEFAULT false | |
| `ai_summary_snapshot` | TEXT | NULLABLE | Snapshot tại thời điểm đặt, KHÔNG tự update sau (BR-AI-06/BR-APT-06) |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT now() | |
| `cancelled_at` | TIMESTAMP | NULLABLE | |

---

## 7. `triage_session` — Phiên AI Triage

| Cột | Kiểu | Ràng buộc | Ghi chú |
|---|---|---|---|
| `id` | BIGSERIAL | PK | |
| `patient_account_id` | BIGINT | FK → `account.id`, NOT NULL | |
| `symptom_description` | TEXT | NOT NULL | Input gốc từ Patient |
| `suggested_department_id` | BIGINT | FK → `department.id`, NULLABLE | Gợi ý, KHÔNG bắt buộc (BR-AI-03) |
| `urgency_level` | VARCHAR(20) | NOT NULL, CHECK IN (`LOW`, `MEDIUM`, `HIGH`, `EMERGENCY`) | |
| `ai_summary` | TEXT | NULLABLE | KHÔNG chứa trường "diagnosis" (BR-AI-02) |
| `ai_execution_id` | BIGINT | FK → `ai_execution.id`, NULLABLE | |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT now() | |

---

## 8. `ai_execution` — Log kỹ thuật mỗi lần gọi AI

| Cột | Kiểu | Ràng buộc | Ghi chú |
|---|---|---|---|
| `id` | BIGSERIAL | PK | |
| `patient_account_id` | BIGINT | FK → `account.id`, NULLABLE | |
| `purpose` | VARCHAR(50) | NOT NULL | VD `TRIAGE` |
| `provider_config_id` | BIGINT | FK → `ai_provider_config.id`, NOT NULL | |
| `model_snapshot` | VARCHAR(100) | NULLABLE | Model thực tế đã dùng tại thời điểm gọi |
| `status` | VARCHAR(20) | NOT NULL, CHECK IN (`SUCCESS`, `FAILED`, `TIMEOUT`) | |
| `request_id` | VARCHAR(100) | NULLABLE | Idempotency key (BR-AIP-04) |
| `input_tokens` | INT | NULLABLE | |
| `output_tokens` | INT | NULLABLE | |
| `sanitized_error` | TEXT | NULLABLE | Lỗi đã lọc, KHÔNG chứa raw response |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT now() | |

**KHÔNG có cột nào lưu nội dung triệu chứng đầy đủ hay raw AI response** — đúng BR-AIP-05.

---

## 9. `ai_provider_config` — Cấu hình AI Provider

| Cột | Kiểu | Ràng buộc | Ghi chú |
|---|---|---|---|
| `id` | BIGSERIAL | PK | |
| `provider` | VARCHAR(50) | NOT NULL | |
| `model` | VARCHAR(100) | NOT NULL | |
| `purpose` | VARCHAR(50) | NOT NULL | |
| `secret_ref` | VARCHAR(255) | NOT NULL | Trỏ tới biến môi trường, KHÔNG lưu raw secret (BR-AIP-03) |
| `enabled` | BOOLEAN | NOT NULL, DEFAULT true | |
| `priority` | INT | NOT NULL, DEFAULT 0 | |
| `timeout_seconds` | INT | NOT NULL | |
| `max_output_tokens` | INT | NOT NULL | |
| `temperature` | DECIMAL(3,2) | NULLABLE | |
| `created_by_account_id` | BIGINT | FK → `account.id`, NOT NULL | Phải là Admin |
| `updated_at` | TIMESTAMP | NULLABLE | |
| `version` | BIGINT | NOT NULL, DEFAULT 0 | Optimistic Locking (EX-AIP-03) |

---

## 10. `audit_log` — Log append-only

| Cột | Kiểu | Ràng buộc | Ghi chú |
|---|---|---|---|
| `id` | BIGSERIAL | PK | |
| `actor_account_id` | BIGINT | NULLABLE, KHÔNG FK cứng | Cho phép giữ log ngay cả khi account bị xoá |
| `action` | VARCHAR(50) | NOT NULL | VD `APPOINTMENT_CREATED` |
| `entity_type` | VARCHAR(50) | NOT NULL | VD `Appointment` |
| `entity_id` | BIGINT | NOT NULL | |
| `metadata` | JSONB | NULLABLE | Chỉ metadata an toàn, KHÔNG chứa password/token/nội dung y tế đầy đủ |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT now() | |

**KHÔNG có cột `updated_at`/`deleted_at`** — bảng chỉ INSERT, không bao giờ UPDATE/DELETE.

---

## Đối chiếu với Ownership Matrix (nhắc lại nhanh)

| Ai | Đọc được gì |
|---|---|
| Patient | `account`/`patient_profile` của chính mình, `triage_session`/`appointment` của chính mình |
| Doctor | `doctor_profile` của chính mình, `appointment`/`ai_summary` **chỉ khi** `share_ai_summary_with_doctor = true` **và** thuộc lịch của mình |
| Admin | `account` (metadata, không đọc password_hash dạng có thể đảo ngược — vốn dĩ hash rồi không đảo được), `department`, `doctor_profile`, `ai_provider_config` — **KHÔNG** đọc `triage_session`/`ai_summary`/`appointment` chi tiết y tế |

---

## Việc cần làm khi bắt đầu CBS-35

- [ ] Migration `V1__create_account_table.sql`
- [ ] Migration `V2__create_patient_profile_table.sql`
- [ ] Entity `Account`, `PatientProfile`
- [ ] `PasswordEncoder` Bean (BCrypt) trong `SecurityConfig`

*(File này cập nhật dần khi có thay đổi thiết kế thật trong lúc code — đừng để lệch với migration thật.)*
