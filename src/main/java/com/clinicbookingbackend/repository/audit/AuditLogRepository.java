package com.clinicbookingbackend.repository.audit;

import com.clinicbookingbackend.entity.audit.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

// Append-only (BR-AIP-05): chỉ dùng save() để tạo mới bản ghi audit_log — KHÔNG thêm method
// update/delete nào vào interface này (JpaRepository vẫn có save()/delete() có sẵn qua kế thừa,
// nhưng theo convention của repo, không Service nào được gọi update/delete trên bảng này).
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
