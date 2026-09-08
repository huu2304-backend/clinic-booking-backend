package com.clinicbookingbackend.entity.audit;

import com.clinicbookingbackend.entity.audit.enums.AuditAction;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// Append-only (BR-AIP-05): không có @Setter và không có field updatedAt/deletedAt — 1 bản ghi
// tạo xong không được sửa. AuditLogRepository cũng chỉ nên dùng save() để tạo mới.
@Entity
@Table(name = "audit_log")
@Getter
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuditAction action;

    // accountId thô của actor thực hiện hành động — có thể null nếu action do hệ thống tự sinh
    // (vd scheduled job), không cần fetch cả entity Account chỉ để ghi log.
    @Column(name = "actor_account_id")
    private Long actorAccountId;

    // entity_type/entity_id là tham chiếu chung (không FK cứng) vì mỗi action trỏ tới 1 bảng
    // nghiệp vụ khác nhau (TriageSession, Appointment, DoctorSchedule, AIProviderConfig...).
    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public AuditLog(AuditAction action, Long actorAccountId, String entityType, Long entityId) {
        this.action = action;
        this.actorAccountId = actorAccountId;
        this.entityType = entityType;
        this.entityId = entityId;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
