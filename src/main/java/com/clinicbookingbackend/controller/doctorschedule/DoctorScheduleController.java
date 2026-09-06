package com.clinicbookingbackend.controller.doctorschedule;

import com.clinicbookingbackend.dto.doctorschedule.DoctorScheduleCreateRequest;
import com.clinicbookingbackend.dto.doctorschedule.DoctorScheduleResponse;
import com.clinicbookingbackend.dto.doctorschedule.DoctorScheduleUpdateRequest;
import com.clinicbookingbackend.service.doctorschedule.DoctorScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Không đặt dưới /api/admin/** vì Doctor cũng phải gọi được endpoint này — phân quyền
// ADMIN/DOCTOR nằm ở SecurityConfig, còn check "chỉ chủ sở hữu" nằm trong Service
// (path-matcher không biểu đạt được luật ownership).
@RestController
@RequestMapping("/api/doctor-schedules")
@RequiredArgsConstructor
public class DoctorScheduleController {

    private final DoctorScheduleService doctorScheduleService;

    @PostMapping
    public ResponseEntity<DoctorScheduleResponse> create(@Valid @RequestBody DoctorScheduleCreateRequest request,
                                                           Authentication authentication) {
        DoctorScheduleResponse response = doctorScheduleService.create(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DoctorScheduleResponse> update(@PathVariable Long id,
                                                          @Valid @RequestBody DoctorScheduleUpdateRequest request,
                                                          Authentication authentication) {
        return ResponseEntity.ok(doctorScheduleService.update(id, request, authentication));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        doctorScheduleService.delete(id, authentication);
        return ResponseEntity.noContent().build();
    }
}
