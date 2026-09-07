package com.clinicbookingbackend.dto.booking;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record AppointmentResponse(
        Long id,
        Long doctorScheduleId,
        Long doctorId,
        String doctorFullName,
        LocalDate workDate,
        LocalTime startTime,
        LocalTime endTime,
        String status,
        LocalDateTime createdAt
) {
}
