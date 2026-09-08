package com.clinicbookingbackend.controller.booking;

import com.clinicbookingbackend.dto.booking.DoctorAppointmentResponse;
import com.clinicbookingbackend.service.booking.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

// CBS-53: Doctor xem Appointment CONFIRMED trong ngày của chính mình (Doctor Dashboard) — luôn
// "me", không nhận doctorId từ path/query nên không thể đụng dữ liệu của Doctor khác.
@RestController
@RequestMapping("/api/doctors/me/appointments")
@RequiredArgsConstructor
public class DoctorAppointmentController {

    private final BookingService bookingService;

    @GetMapping
    public ResponseEntity<List<DoctorAppointmentResponse>> getMyAppointments(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {
        return ResponseEntity.ok(bookingService.getMyAppointments(date, authentication));
    }
}
