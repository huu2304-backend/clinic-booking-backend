-- audit_log: append-only (BR-AIP-05) — KHÔNG có cột updated_at/deleted_at, không có API/service
-- nào trong repo được phép UPDATE/DELETE bảng này, chỉ INSERT.
--
-- SỐ THỨ TỰ: đặt V10 (bỏ qua V9) vì V9 đã được PR #17 (CBS-66, thêm cột address vào
-- patient_profile) dùng nhưng chưa merge vào develop tại thời điểm tạo migration này — tránh
-- đụng version khi cả 2 PR cùng merge. Flyway không yêu cầu version liên tục nên an toàn.
--
-- action giới hạn qua CHECK theo đúng tập tối thiểu yêu cầu ở US-AIP-02; khi có action mới
-- (Sprint AI Triage/AI Provider Admin) sẽ DROP + ADD lại constraint này ở 1 migration sau,
-- không sửa file này (Flyway forward-only).
--
-- Cố tình KHÔNG có cột lưu nội dung tự do (vd "details"/"payload") để tránh vô tình chứa
-- password/token/raw secret/nội dung triệu chứng đầy đủ — chỉ tham chiếu entity_type/entity_id,
-- không có FK cứng tới entity_id vì mỗi action trỏ tới 1 bảng nghiệp vụ khác nhau
-- (TriageSession, Appointment, DoctorSchedule, AIProviderConfig...).
CREATE TABLE audit_log
(
    id               BIGSERIAL PRIMARY KEY,
    action           VARCHAR(50) NOT NULL,
    actor_account_id BIGINT,
    entity_type      VARCHAR(50),
    entity_id        BIGINT,
    created_at       TIMESTAMP   NOT NULL DEFAULT now(),

    CONSTRAINT fk_audit_log_actor_account FOREIGN KEY (actor_account_id) REFERENCES account (id),
    CONSTRAINT chk_audit_log_action CHECK (action IN (
        'TRIAGE_SESSION_CREATED',
        'APPOINTMENT_CREATED',
        'APPOINTMENT_CANCELLED',
        'SCHEDULE_CREATED',
        'AI_PROVIDER_UPDATED'
        ))
);

CREATE INDEX idx_audit_log_actor_account_id ON audit_log (actor_account_id);
CREATE INDEX idx_audit_log_action ON audit_log (action);
