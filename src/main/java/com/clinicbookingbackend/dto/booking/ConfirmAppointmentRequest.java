package com.clinicbookingbackend.dto.booking;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmAppointmentRequest {

    @NotNull(message = "Slot lịch làm việc không được để trống")
    private Long doctorScheduleId;
}
