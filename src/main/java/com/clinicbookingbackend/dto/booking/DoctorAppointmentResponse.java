package com.clinicbookingbackend.dto.booking;

import java.time.LocalDate;
import java.time.LocalTime;

// CBS-53: chỉ hiển thị giờ khám + tên bệnh nhân — KHÔNG có field dữ liệu y tế
// (aiSummary thuộc US-DOC-02/CBS-54, chưa mở ở đây).
public record DoctorAppointmentResponse(
        Long appointmentId,
        LocalDate workDate,
        LocalTime startTime,
        LocalTime endTime,
        String patientFullName,
        String status
) {
}
