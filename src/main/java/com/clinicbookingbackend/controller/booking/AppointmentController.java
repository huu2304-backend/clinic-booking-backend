package com.clinicbookingbackend.controller.booking;

import com.clinicbookingbackend.dto.booking.AppointmentResponse;
import com.clinicbookingbackend.dto.booking.ConfirmAppointmentRequest;
import com.clinicbookingbackend.service.booking.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<AppointmentResponse> confirm(@Valid @RequestBody ConfirmAppointmentRequest request,
                                                         Authentication authentication) {
        AppointmentResponse response = bookingService.confirm(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
