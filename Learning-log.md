# Nhật ký học tập — AI-Powered Clinic Booking System

> File này KHÔNG phải tài liệu kỹ thuật của dự án (đã có `README.md` lo việc đó).
> File này là **của riêng bạn** — nơi ghi lại thứ bạn học được, bug bạn gặp, và bạn nghĩ gì sau mỗi Task.
> Không cần viết hay, không cần đúng chuẩn — viết bằng lời của chính bạn để 3 tháng sau đọc lại vẫn hiểu ngay.

## Cách dùng

- Mỗi khi học 1 khái niệm/annotation mới → thêm 1 dòng vào bảng "Khái niệm đã học"
- Mỗi khi gặp bug và tự fix được (hoặc được hướng dẫn fix) → thêm 1 dòng vào bảng "Bug đã gặp"
- Xong mỗi Task Jira → viết vài dòng phản tư ở cuối file (mục "Phản tư sau mỗi Task")
- Không cần commit file này lên Git nếu không muốn — đây là ghi chép cá nhân, không phải deliverable

---

## 📚 Khái niệm / Annotation đã học

| Ngày | Khái niệm | Giải thích ngắn (bằng lời của mình) | Task |
|---|---|---|---|
| 2026-08 | `@Configuration` | Đánh dấu 1 class là nơi định nghĩa Bean cho Spring, Spring quét class này lúc khởi động | Setup |
| 2026-08 | `@Bean` | Method trả về 1 object mà mình muốn Spring quản lý (đưa vào Spring Container) thay vì tự `new` | Setup |
| 2026-08 | `ddl-auto=validate` | Hibernate CHỈ kiểm tra Entity có khớp bảng DB không, KHÔNG tự tạo/sửa bảng — vì mình dùng Flyway để tạo bảng, không để 2 cơ chế đá nhau | Setup |
| | `@Getter` (Lombok) | *(đang học ở CBS-64 — tự viết định nghĩa của bạn vào đây sau khi đọc xong)* | CBS-64 |
| | | | |

*(thêm dòng mới ở đây khi học thêm)*

---

## 🐛 Bug đã gặp & cách fix

| Ngày | Bug | Nguyên nhân gốc | Cách fix |
|---|---|---|---|
| 2026-08 | Docker: `open //./pipe/docker_engine...` | Docker Desktop chưa mở/daemon chưa chạy | Mở Docker Desktop, chờ "Engine running" rồi mới chạy lệnh |
| 2026-08 | Maven: `Unrecognised tag: 'n'` trong `pom.xml` | Gõ nhầm `<n>` thay vì `<name>` | Sửa lại đúng tag `<name>` |
| 2026-08 | `MalformedInputException: Input length = 1` khi `mvn spring-boot:run` | File `application.properties` bị lưu sai encoding (không phải UTF-8) — thường do gõ comment tiếng Việt có dấu mà IntelliJ chưa set encoding UTF-8 | Set File Encoding = UTF-8 trong IntelliJ Settings; `git checkout --` để lấy lại bản sạch nếu file đã hỏng |
| 2026-08 | Port 8080 already in use | Chạy `mvn spring-boot:run` 2 lần (1 lần ở terminal cũ chưa tắt, 1 lần ở IntelliJ) | `Ctrl+C` tiến trình cũ, hoặc `netstat -ano \| findstr :8080` rồi `taskkill /PID xxx /F` |
| 2026-08 | Lộ mật khẩu thật (`23042003`) lên GitHub public | Để giá trị thật làm fallback default trong `${DB_PASSWORD:23042003}` rồi commit | Không bao giờ để giá trị thật trong file có khả năng commit — chỉ để tên biến, giá trị thật nằm trong `.env` (đã gitignore) |
| 2026-08 | Import sai `Info`/`SecurityScheme` trong `SwaggerConfig` | IntelliJ auto-import nhầm package `io.swagger.v3.oas.annotations.*` (annotation type) thay vì `io.swagger.v3.oas.models.*` (model/builder object) — 2 package có class trùng tên | Sửa lại đúng import từ `oas.models.*` |
| | | | | Dùng @Builder để tạo contructor cho class `Info`/`SecurityScheme` thay vì dùng constructor mặc định (vì constructor mặc định không có tham số, không set được field) |

*(thêm dòng mới ở đây khi gặp bug mới)*

---

## 🤔 Phản tư sau mỗi Task

### Setup môi trường (trước CBS-64)
- **Học được gì:** *(tự viết)*
- **Nếu làm lại sẽ khác gì:** *(tự viết — gợi ý: có nên set encoding UTF-8 và tạo `.env` ngay từ phút đầu tiên, trước khi code dòng nào không?)*

### CBS-64 — Global Exception Handler
- **Học được gì:**
- **Chỗ nào lúc đầu hiểu sai, sau mới vỡ ra:**
- **Nếu làm lại sẽ khác gì:**

### CBS-XX — ...
- ...

---

## 💡 Ghi chú tư duy chung (rút ra dần trong quá trình làm)

- *(VD: "Code từ class không phụ thuộc gì → class phụ thuộc nhiều nhất, build sau mỗi bước nhỏ" — rút ra từ lúc bắt đầu CBS-64)*
- *(thêm dần)*
