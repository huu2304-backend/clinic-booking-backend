package com.clinicbookingbackend.dto.doctor;

public record DoctorSummaryResponse(
        Long id,
        String fullName,
        String departmentName,
        
        Long departmentId
) {

}
