package com.clinicbookingbackend.dto.doctorschedule;

import com.clinicbookingbackend.entity.doctorschedule.enums.ScheduleStatus;

import java.time.LocalDate;
import java.time.LocalTime;

public record DoctorScheduleResponse(
        Long id,
        Long doctorId,
        String doctorFullName,
        LocalDate workDate,
        LocalTime startTime,
        LocalTime endTime,
        ScheduleStatus status
) {
}
