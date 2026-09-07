package com.clinicbookingbackend.dto.booking;

import java.time.LocalDateTime;

public record HoldResponse(
        Long doctorScheduleId,
        LocalDateTime lockExpiresAt
) {
}
