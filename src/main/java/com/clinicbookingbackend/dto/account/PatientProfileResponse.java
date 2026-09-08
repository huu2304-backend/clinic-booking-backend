package com.clinicbookingbackend.dto.account;

import com.clinicbookingbackend.entity.account.enums.Gender;

import java.time.LocalDate;

public record PatientProfileResponse(
        Long id,
        String email,
        String fullName,
        LocalDate dateOfBirth,
        Gender gender,
        String phoneNumber,
        String address
) {
}
