package com.clinicbookingbackend.controller.booking;

import com.clinicbookingbackend.dto.booking.HoldResponse;
import com.clinicbookingbackend.service.booking.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Patient giữ chỗ/hủy giữ chỗ 1 slot (CBS-72) — khác /api/doctor-schedules/** (Admin/Doctor quản
// lý slot) và /api/doctors/{id}/schedules (Patient xem slot trống, CBS-39).
@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class SlotHoldController {

    private final BookingService bookingService;

    @PostMapping("/{id}/hold")
    public ResponseEntity<HoldResponse> hold(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(bookingService.hold(id, authentication));
    }

    @PostMapping("/{id}/release-hold")
    public ResponseEntity<Void> releaseHold(@PathVariable Long id, Authentication authentication) {
        bookingService.releaseHold(id, authentication);
        return ResponseEntity.noContent().build();
    }
}
