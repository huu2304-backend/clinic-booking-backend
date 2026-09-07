package com.clinicbookingbackend.controller.department;

import com.clinicbookingbackend.dto.doctor.DoctorSummaryResponse;
import com.clinicbookingbackend.service.doctor.DoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Không đặt dưới /api/admin/** — endpoint này dành cho Patient xem danh sách bác sĩ theo
// khoa (CBS-39/US-SCH-02), khác DepartmentController (admin CRUD khoa).
@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class PatientDepartmentController {

    private final DoctorService doctorService;

    @GetMapping("/{id}/doctors")
    public ResponseEntity<Page<DoctorSummaryResponse>> getActiveDoctorsByDepartment(@PathVariable Long id,
                                                                                      Pageable pageable) {
        return ResponseEntity.ok(doctorService.getActiveDoctorsByDepartment(id, pageable));
    }
}
