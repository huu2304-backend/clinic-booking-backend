package com.clinicbookingbackend.repository;

import com.clinicbookingbackend.entity.account.PatientProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientProfileRepository extends JpaRepository<PatientProfile, Long> {
    Optional<PatientProfile> findByAccountId(Long accountId);
}