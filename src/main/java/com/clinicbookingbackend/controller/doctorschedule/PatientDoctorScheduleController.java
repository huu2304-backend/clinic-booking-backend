package com.clinicbookingbackend.controller.doctorschedule;

import com.clinicbookingbackend.dto.doctorschedule.AvailableSlotResponse;
import com.clinicbookingbackend.service.doctorschedule.DoctorScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

// Không đặt dưới /api/doctor-schedules/** — endpoint này dành cho Patient xem slot trống
// của 1 bác sĩ theo ngày (CBS-39/US-SCH-02), khác DoctorScheduleController (Admin/Doctor
// quản lý slot).
@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class PatientDoctorScheduleController {

    private final DoctorScheduleService doctorScheduleService;

    @GetMapping("/{id}/schedules")
    public ResponseEntity<List<AvailableSlotResponse>> getAvailableSlots(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(doctorScheduleService.getAvailableSlots(id, date));
    }
}
