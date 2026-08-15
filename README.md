# AI-Powered Clinic Booking System (CBS)

> Hệ thống đặt lịch khám bệnh thông minh — chống trùng lịch hẹn (Booking Conflict) và tự động hóa phân luồng bệnh nhân bằng AI Triage.
>
![Status](https://img.shields.io/badge/status-in%20development-yellow)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.x-green)
![React](https://img.shields.io/badge/React-18.x-61DAFB)

---

## Mục lục

- [1. Tổng quan](#1-tổng-quan)
- [2. Kiến trúc & Tech Stack](#2-kiến-trúc--tech-stack)
- [3. Tính năng chính](#3-tính-năng-chính)
- [4. Business Rules cốt lõi](#4-business-rules-cốt-lõi)
- [5. Cấu trúc thư mục](#5-cấu-trúc-thư-mục)
- [6. Yêu cầu môi trường](#6-yêu-cầu-môi-trường)
- [7. Cài đặt & Chạy dự án](#7-cài-đặt--chạy-dự-án)
- [8. Biến môi trường](#8-biến-môi-trường)
- [9. API Documentation](#9-api-documentation)
- [10. Database Schema (tóm tắt)](#10-database-schema-tóm-tắt)
- [11. Quy trình phát triển (Git Flow)](#11-quy-trình-phát-triển-git-flow)
- [12. Roadmap Sprint](#12-roadmap-sprint)
- [13. Testing](#13-testing)
- [14. Deployment](#14-deployment)
- [15. Đóng góp / Ghi chú cho bản thân](#15-đóng-góp--ghi-chú-cho-bản-thân)

---

## 1. Tổng quan

**CBS** là hệ thống đặt lịch khám bệnh giải quyết 2 bài toán chính:

1. **Chống trùng lịch hẹn (Booking Conflict):** đảm bảo 2 bệnh nhân không thể cùng đặt trùng 1 slot khám của bác sĩ, kể cả khi request gửi lên gần như đồng thời (concurrency-safe bằng Optimistic Locking + cơ chế giữ chỗ tạm thời `LOCKED`).
2. **Tự động hóa phân luồng bệnh nhân (AI Triage):** bệnh nhân mô tả triệu chứng qua chat, AI (LLM) gợi ý khoa khám phù hợp, đánh giá mức độ khẩn cấp và tóm tắt bệnh án gửi cho bác sĩ trước khi khám.

### Các vai trò (Role)

| Role | Quyền hạn chính |
|---|---|
| **Patient** | Đăng ký/đăng nhập, chat AI Triage, xem slot trống, đặt lịch, hủy lịch, xem/sửa hồ sơ cá nhân |
| **Doctor** | Xem lịch khám trong ngày của mình, đọc tóm tắt AI (nếu Patient đồng ý chia sẻ) |
| **Admin** | Quản lý Department/Doctor, quản lý cấu hình AI Provider — **không** có quyền đọc dữ liệu y tế của Patient dù ở chế độ debug |

---

## 2. Kiến trúc & Tech Stack

```
┌─────────────┐     REST API      ┌──────────────────┐     JDBC      ┌────────────┐
│   React SPA │ ───────────────►  │  Spring Boot API │ ────────────► │ PostgreSQL │
│  (WebStorm) │  ◄─────────────── │   (IntelliJ)     │ ◄──────────── │            │
└─────────────┘      JSON         └──────────────────┘               └────────────┘
                                            │
                                            ▼
                                   ┌──────────────────┐
                                   │  AI Provider API  │
                                   │   (LLM, qua       │
                                   │  AIProviderConfig)│
                                   └──────────────────┘
```

| Layer | Công nghệ |
|---|---|
| Backend | Java 21, Spring Boot 4.1.x, Spring Web MVC, Spring Data JPA, Spring Security |
| Frontend | JavaScript, ReactJS 18.x, Axios, React Router |
| Database | **PostgreSQL** (chạy qua Docker container — đồng nhất giữa các máy dev, không cài local qua XAMPP; quản trị bằng Azure Data Studio hoặc pgAdmin) |
| Migration | Flyway |
| API Docs | springdoc-openapi (Swagger UI) |
| Auth | JWT (JSON Web Token) |
| Concurrency Control | Optimistic Locking (`@Version`) + trạng thái `LOCKED` có TTL |
| Containerization | Docker, Docker Compose |
| Deployment | Render |
| CI | GitHub Actions |
| Quản lý task | Jira (Scrum: Backlog → Sprint 1-5) |
| Version Control | GitHub, Git Flow (`main` / `develop` / `feature/*`) |

> **Lưu ý:** Dự án ban đầu định dùng MySQL (qua XAMPP), sau đó đổi hẳn sang PostgreSQL chạy bằng Docker để đảm bảo mọi máy dev (kể cả sau này thêm người) dùng chung 1 phiên bản DB giống hệt nhau, tránh lỗi "chạy máy tôi thì được". Azure Data Studio vẫn dùng được để quản trị PostgreSQL qua extension.

---

## 3. Tính năng chính

- [ ] **Auth:** Đăng ký/Đăng nhập Patient, JWT-based authentication
- [ ] **Quản lý Department & Doctor** (Admin)
- [ ] **Quản lý lịch làm việc bác sĩ** (Doctor Schedule)
- [ ] **AI Triage:** Chat mô tả triệu chứng → gợi ý khoa khám + mức độ khẩn cấp + tóm tắt bệnh án
- [ ] **Đặt lịch concurrency-safe:** giữ chỗ tạm thời (`LOCKED`, có TTL) → xác nhận (`BOOKED`)
- [ ] **Hủy lịch hẹn** (trong khung thời gian cho phép)
- [ ] **Doctor Dashboard:** xem lịch khám trong ngày + đọc tóm tắt AI
- [ ] **Quản lý AI Provider Config** (Admin) — CRUD, mask secret, Audit Log
- [ ] **Profile Management** (Patient tự cập nhật hồ sơ)

> Checklist trên được cập nhật tay theo tiến độ code thật — không đồng bộ tự động với Jira.

---

## 4. Business Rules cốt lõi

Chi tiết đầy đủ nằm ở tài liệu đặc tả nghiệp vụ riêng (`docs/business-rules.docx`). Một số rule quan trọng nhất ảnh hưởng trực tiếp tới kiến trúc code:

| Mã BR | Nội dung |
|---|---|
| BR-SCH-01 | `DoctorSchedule.status` ∈ {`AVAILABLE`, `LOCKED`, `BOOKED`, `CANCELLED`} |
| BR-SCH-05 | Slot `LOCKED` phải có TTL, hết hạn tự động trả về `AVAILABLE` qua scheduled job |
| BR-APT-01 | Không được có 2 Appointment `CONFIRMED` cùng 1 `doctor_schedule_id` (UNIQUE constraint tầng DB) |
| BR-APT-02 | Chuyển trạng thái slot phải dùng Optimistic Locking (`@Version`) |
| BR-APT-04 | Chỉ hủy lịch được nếu còn cách giờ khám tối thiểu N giờ (cấu hình, không hardcode) |
| BR-APT-05 | Hủy lịch là 1 transaction: cập nhật `Appointment` + `DoctorSchedule` cùng lúc, rollback toàn bộ nếu lỗi |
| BR-AI-02 | Output AI chỉ chứa `suggestedDepartment` / `urgencyLevel` / `aiSummary` — **không** có trường "diagnosis" |
| BR-AI-03 | `suggestedDepartment` chỉ là gợi ý, Patient luôn được tự do chọn khoa khác |
| BR-AI-05 | Input của Patient luôn được coi là *data*, không được dùng để điều khiển system prompt (chống prompt injection) |
| BR-AI-07 | Output AI phải qua JSON-schema validation trước khi lưu |
| BR-AIP-01/02/03 | Chỉ Admin CRUD AIProviderConfig; Admin **không** đọc được dữ liệu y tế; secret luôn bị mask |

---

## 5. Cấu trúc thư mục

```
clinic-booking-system/
├── backend/                     # Spring Boot (IntelliJ IDEA)
│   ├── src/main/java/com/clinicbookingbackend/
│   │   ├── config/               # Security, Swagger, CORS config
│   │   ├── controller/           # REST Controllers
│   │   ├── service/               # Business logic
│   │   ├── repository/            # Spring Data JPA repositories
│   │   ├── entity/                 # JPA Entities
│   │   ├── dto/                     # Request/Response DTOs
│   │   ├── exception/               # Custom exceptions + GlobalExceptionHandler
│   │   ├── security/                 # JWT filter, UserDetailsService
│   │   └── scheduler/                 # Scheduled jobs (VD: dọn LOCKED hết hạn)
│   ├── src/main/resources/
│   │   ├── db/migration/          # Flyway migration scripts (V1__..., V2__...)
│   │   └── application.properties
│   └── pom.xml
├── frontend/                    # ReactJS (WebStorm)
│   ├── src/
│   │   ├── pages/
│   │   ├── components/
│   │   ├── api/                   # Axios instance + interceptors
│   │   ├── context/                # Auth context, JWT state
│   │   └── routes/
│   └── package.json
├── docs/
│   └── business-rules.docx        # Đặc tả nghiệp vụ gốc
├── docker-compose.yml
├── .env.example
└── README.md
```

*(Cấu trúc trên là chuẩn đề xuất — cập nhật lại phần này khi bạn tạo project thật trong IntelliJ nếu có khác biệt.)*

---

## 6. Yêu cầu môi trường

- JDK 21
- Maven 3.8+
- Node.js 18+ / npm
- Docker & Docker Compose (chạy PostgreSQL local — xem `docker-compose.yml`, không cần cài DB trực tiếp lên máy)
- IntelliJ IDEA (backend), WebStorm (frontend)

---

## 7. Cài đặt & Chạy dự án

### 7.1. Chạy Backend (local)

```bash
# 1. Khởi động PostgreSQL bằng Docker trước
cp .env.example .env   # rồi sửa DB_PASSWORD trong .env
docker-compose up -d postgres

# 2. Chạy backend
cd backend
mvn clean install
mvn spring-boot:run
```

Backend mặc định chạy tại: `http://localhost:8080`
Swagger UI: `http://localhost:8080/swagger-ui/index.html`

### 7.2. Chạy Frontend (local)

```bash
cd frontend
npm install
npm start
```

Frontend mặc định chạy tại: `http://localhost:3000`

### 7.3. Chạy toàn bộ bằng Docker Compose (khuyến nghị khi demo)

```bash
docker-compose up --build
```

Lệnh này sẽ khởi động đồng thời: `backend` + `frontend` + `postgres` (backend/frontend service sẽ được thêm vào `docker-compose.yml` ở Sprint 5 — hiện tại file chỉ có service `postgres` phục vụ dev local).

---

## 8. Biến môi trường

Tạo file `.env` ở root (copy từ `.env.example`), **không commit file `.env` thật lên Git** (đã có trong `.gitignore`).

| Biến | Mô tả | Ví dụ |
|---|---|---|
| `DB_NAME` | Tên database PostgreSQL | `cbs_db` |
| `DB_USERNAME` | Username DB | `postgres` |
| `DB_PASSWORD` | Password DB | `********` |
| `JWT_SECRET` | Secret key ký JWT | `********` |
| `JWT_EXPIRATION` | Thời gian hết hạn token (ms) | `86400000` |
| `AI_PROVIDER_API_KEY` | API key gọi AI Provider (LLM) | `********` |
| `AI_PROVIDER_ENDPOINT` | Endpoint API của AI Provider | `https://api.example.com/v1` |
| `SLOT_LOCK_TTL_MINUTES` | TTL giữ chỗ slot trước khi tự trả `AVAILABLE` | `5` |
| `CANCEL_MIN_HOURS_BEFORE` | Số giờ tối thiểu trước giờ khám được phép hủy | `24` |

`application.properties` đọc các biến DB qua cú pháp `${DB_NAME:cbs_db}` (có giá trị mặc định khi chạy local không qua Docker).

> Danh sách này sẽ tăng dần theo tiến độ code — cập nhật ngay khi thêm biến môi trường mới, đừng để README lệch với `application.properties` thật.

---

## 9. API Documentation

Toàn bộ API được document tự động qua **springdoc-openapi**, truy cập khi backend đang chạy:

```
http://localhost:8080/swagger-ui/index.html
```

Quy trình test API: **luôn test qua Swagger UI trước khi code Frontend** cho tính năng đó (đảm bảo API đúng hành vi/response trước khi ràng buộc UI vào).

Format lỗi chuẩn hóa toàn hệ thống (qua `GlobalExceptionHandler`):

```json
{
  "timestamp": "2026-08-15T10:00:00",
  "status": 409,
  "errorCode": "SLOT_UNAVAILABLE",
  "message": "Slot đã được đặt hoặc đang được giữ bởi người khác",
  "path": "/api/schedules/123/hold"
}
```

---

## 10. Database Schema (tóm tắt)

| Bảng | Mô tả |
|---|---|
| `patient` / `patient_profile` | Tài khoản & hồ sơ Patient |
| `doctor` | Tài khoản Doctor, thuộc 1 `department` |
| `department` | Khoa khám |
| `doctor_schedule` | Slot khám: `status` (`AVAILABLE/LOCKED/BOOKED/CANCELLED`), `locked_by_patient_id`, `lock_expires_at`, `version` |
| `appointment` | Lịch hẹn đã xác nhận, UNIQUE theo `doctor_schedule_id` |
| `triage_session` | Phiên chat AI Triage, `suggested_department_id`, `urgency_level`, `ai_summary` |
| `ai_execution` | Log kỹ thuật mỗi lần gọi AI (không lưu nội dung triệu chứng đầy đủ) |
| `ai_provider_config` | Cấu hình AI Provider (model, timeout, priority...), `secret_ref` được mask |
| `audit_log` | Log append-only các action nghiệp vụ chính |

*(Schema chi tiết từng cột nằm trong migration script `db/migration/` — đây chỉ là bản tóm tắt tham khảo nhanh.)*

---

## 11. Quy trình phát triển (Git Flow)

```
main        ─────●───────────────●──────────►  (production, tag theo version)
                  \               \
develop     ───●───●───●───●───●───●─────────►  (tích hợp các feature)
             \   \       \
feature/*     ●   ●       ●                      (1 nhánh / 1 task Jira)
```

- Nhánh đặt tên: `feature/CBS-XX-mo-ta-ngan` (VD: `feature/CBS-64-global-exception-handler`)
- Tạo nhánh từ `develop`, PR merge lại vào `develop`
- Commit message gợi ý: `feat(CBS-64): add global exception handler`
- Chỉ merge `develop` → `main` khi kết thúc 1 Sprint và đã test end-to-end

---

## 12. Roadmap Sprint

| Sprint | Nội dung | Trạng thái |
|---|---|---|
| **Sprint 1** | Core Booking Flow: Auth, Department/Doctor, Schedule, Booking concurrency-safe (LOCKED→BOOKED), Profile, Swagger, Seed data, FE cơ bản | 🔲 Chưa bắt đầu |
| **Sprint 2** | AI Triage Integration: chat triệu chứng, gợi ý khoa, AIProviderConfig (DB), FE chat UI | 🔲 |
| **Sprint 3** | Appointment Lifecycle: hủy lịch, Doctor Dashboard, FE tương ứng | 🔲 |
| **Sprint 4** | AI Provider Administration: CRUD config, Audit Log, regression test toàn MVP | 🔲 |
| **Sprint 5** | Containerization & Deployment: Docker, docker-compose, Render, CI | 🔲 |

> Chi tiết Task/Acceptance Criteria quản lý đầy đủ trên Jira project **CBS**. README này chỉ tóm tắt ở mức Epic.

---

## 13. Testing

- Unit test: JUnit 5 + Mockito (backend)
- Integration test: `@SpringBootTest` cho các luồng có Transaction/concurrency quan trọng (đặc biệt CBS-43: test race condition khi nhiều request cùng giữ chỗ 1 slot)
- Frontend: (bổ sung sau khi chọn công cụ — VD Jest/React Testing Library)

---

## 14. Deployment

Xem chi tiết Sprint 5 (Docker + Render). Sau khi deploy, cập nhật link production tại đây:

- **Backend (Render):** `<sẽ cập nhật>`
- **Frontend (Render):** `<sẽ cập nhật>`
- **Swagger production:** `<sẽ cập nhật>/swagger-ui/index.html`

---

## 15. Đóng góp / Ghi chú cho bản thân

Dự án cá nhân, 1 Fullstack Developer duy nhất (Quang Hữu) — không có quy trình PR review chéo, nhưng vẫn tuân thủ Git Flow + Jira để luyện tập đúng chuẩn doanh nghiệp trước khi ứng tuyển vị trí Java Web Developer.

**Việc cần làm mỗi khi hoàn thành 1 Task:**
1. Code xong → tự test qua Swagger
2. Merge `feature/*` vào `develop`
3. Cập nhật checklist ở mục [3](#3-tính-năng-chính) và trạng thái Sprint ở mục [12](#12-roadmap-sprint)
4. Transition issue tương ứng trên Jira sang Done

---

*README này là tài liệu sống — cập nhật liên tục theo tiến độ thật, không để lệch giữa code và mô tả.*
