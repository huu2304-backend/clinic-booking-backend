package com.clinicbookingbackend.repository.audit;

import com.clinicbookingbackend.entity.account.Account;
import com.clinicbookingbackend.entity.account.enums.Role;
import com.clinicbookingbackend.entity.account.enums.Status;
import com.clinicbookingbackend.entity.audit.AuditLog;
import com.clinicbookingbackend.entity.audit.enums.AuditAction;
import com.clinicbookingbackend.repository.account.AccountRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// CBS-56: chứng minh audit_log thực sự append-only (BR-AIP-05) và action bị CHECK constraint
// chặn ở tầng DB — insert/update thẳng qua repository/EntityManager, bỏ qua toàn bộ service
// layer (mirror AppointmentRepositoryConstraintTest, US-BOOK-01.1). Cần Postgres đang chạy
// (docker compose up -d postgres).
@SpringBootTest
class AuditLogRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @PersistenceContext
    private EntityManager entityManager;

    private Account actorAccount;
    private Long auditLogId;

    @AfterEach
    void tearDown() {
        if (auditLogId != null) {
            auditLogRepository.deleteById(auditLogId);
        }
        if (actorAccount != null) {
            accountRepository.deleteById(actorAccount.getId());
        }
    }

    @Test
    void save_shouldPersistAuditLog_withGeneratedCreatedAt() {
        actorAccount = accountRepository.save(newAccount());

        AuditLog auditLog = new AuditLog(
                AuditAction.APPOINTMENT_CREATED, actorAccount.getId(), "Appointment", 123L);
        auditLog = auditLogRepository.saveAndFlush(auditLog);
        auditLogId = auditLog.getId();

        AuditLog reloaded = auditLogRepository.findById(auditLogId).orElseThrow();
        assertThat(reloaded.getAction()).isEqualTo(AuditAction.APPOINTMENT_CREATED);
        assertThat(reloaded.getActorAccountId()).isEqualTo(actorAccount.getId());
        assertThat(reloaded.getEntityType()).isEqualTo("Appointment");
        assertThat(reloaded.getEntityId()).isEqualTo(123L);
        assertThat(reloaded.getCreatedAt()).isNotNull();
    }

    @Test
    void save_shouldAllowNullActorAccountId_forSystemGeneratedAction() {
        AuditLog auditLog = new AuditLog(AuditAction.SCHEDULE_CREATED, null, "DoctorSchedule", 456L);
        auditLog = auditLogRepository.saveAndFlush(auditLog);
        auditLogId = auditLog.getId();

        assertThat(auditLogRepository.findById(auditLogId).orElseThrow().getActorAccountId()).isNull();
    }

    @Test
    void insertingInvalidAction_shouldViolateDbCheckConstraint_evenBypassingJavaEnum() {
        // AuditAction (Java enum) không cho phép tạo giá trị ngoài 5 action hợp lệ — insert
        // thẳng qua native SQL để chứng minh CHECK constraint nằm ở tầng DB, không chỉ ở Java.
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> transactionTemplate.execute(status -> {
            entityManager.createNativeQuery(
                            "INSERT INTO audit_log (action, created_at) VALUES ('NOT_A_REAL_ACTION', now())")
                    .executeUpdate();
            return null;
        })).satisfies(ex -> assertThat(rootCauseMessage(ex)).contains("chk_audit_log_action"));
    }

    private String rootCauseMessage(Throwable ex) {
        Throwable root = ex;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getMessage();
    }

    private Account newAccount() {
        Account account = new Account();
        account.setEmail("audit-log-test-" + System.nanoTime() + "@test.local");
        account.setPasswordHash("N/A");
        account.setRole(Role.ADMIN);
        account.setStatus(Status.ACTIVE);
        return account;
    }
}
