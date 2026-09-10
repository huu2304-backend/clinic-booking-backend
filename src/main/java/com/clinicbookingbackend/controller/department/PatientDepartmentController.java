package com.clinicbookingbackend.controller.department;

import com.clinicbookingbackend.dto.department.DepartmentResponse;
import com.clinicbookingbackend.dto.doctor.DoctorSummaryResponse;
import com.clinicbookingbackend.service.department.DepartmentService;
import com.clinicbookingbackend.service.doctor.DoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Không đặt dưới /api/admin/** — endpoint này dành cho Patient xem danh sách khoa + bác sĩ theo
// khoa (CBS-39/US-SCH-02), khác DepartmentController (admin CRUD khoa, chỉ ADMIN gọi được).
@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class PatientDepartmentController {

    private final DoctorService doctorService;
    private final DepartmentService departmentService;

    // Bước 1 của luồng đặt lịch (CBS-68): Patient/Doctor/Admin đã login đều xem được danh sách
    // khoa để bắt đầu chọn dịch vụ khám — tái dùng DepartmentService.getAll() (đã dùng bởi
    // DepartmentController/admin), chỉ khác endpoint không yêu cầu role ADMIN.
    @GetMapping
    public ResponseEntity<List<DepartmentResponse>> getAll() {
        return ResponseEntity.ok(departmentService.getAll());
    }

    @GetMapping("/{id}/doctors")
    public ResponseEntity<Page<DoctorSummaryResponse>> getActiveDoctorsByDepartment(@PathVariable Long id,
                                                                                      Pageable pageable) {
        return ResponseEntity.ok(doctorService.getActiveDoctorsByDepartment(id, pageable));
    }
}
