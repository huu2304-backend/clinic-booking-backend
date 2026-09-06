package com.clinicbookingbackend.dto.doctor;

import com.clinicbookingbackend.entity.account.enums.Status;

public record DoctorResponse(
         Long id,
         String fullName,
         String email,
         Long departmentId,
         Status status,
         String departmentName
) {
}
