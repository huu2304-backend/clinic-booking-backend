package com.clinicbookingbackend.repository.account;

import com.clinicbookingbackend.entity.account.PatientProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PatientProfileRepository extends JpaRepository<PatientProfile, Long> {
    Optional<PatientProfile> findByAccountId(Long accountId);

    // Dùng để resolve tên bệnh nhân theo lô (CBS-53) — Appointment chỉ lưu patientAccountId thô
    // (không có quan hệ JPA tới PatientProfile), nên phải query gộp 1 lần thay vì N+1 query.
    List<PatientProfile> findByAccountIdIn(List<Long> accountIds);
}