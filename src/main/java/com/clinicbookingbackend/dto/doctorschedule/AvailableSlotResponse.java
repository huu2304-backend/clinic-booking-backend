package com.clinicbookingbackend.dto.doctorschedule;

import java.time.LocalDate;
import java.time.LocalTime;

public record AvailableSlotResponse(
        Long id,
        LocalDate workDate,
        LocalTime startTime,
        LocalTime endTime
) {

}
