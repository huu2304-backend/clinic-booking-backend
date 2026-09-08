package com.clinicbookingbackend.entity.audit.enums;

// Tập action tối thiểu theo AC CBS-56/BR-AIP-05. Thêm action mới ở đây PHẢI kèm 1 migration mới
// cập nhật lại CHECK constraint chk_audit_log_action (Flyway forward-only, không sửa V10).
public enum AuditAction {
    TRIAGE_SESSION_CREATED,
    APPOINTMENT_CREATED,
    APPOINTMENT_CANCELLED,
    SCHEDULE_CREATED,
    AI_PROVIDER_UPDATED
}
