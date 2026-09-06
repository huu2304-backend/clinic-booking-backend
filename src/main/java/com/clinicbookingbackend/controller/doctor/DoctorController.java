package com.clinicbookingbackend.controller.doctor;

import com.clinicbookingbackend.dto.doctor.DoctorCreateRequest;
import com.clinicbookingbackend.dto.doctor.DoctorResponse;
import com.clinicbookingbackend.dto.doctor.DoctorUpdateRequest;
import com.clinicbookingbackend.service.doctor.DoctorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;

    @PostMapping
    public ResponseEntity<DoctorResponse> create(@Valid @RequestBody DoctorCreateRequest request) {
        DoctorResponse response = doctorService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<DoctorResponse>> getAll(Pageable pageable) {
        return ResponseEntity.ok(doctorService.getAll(pageable));
    }

    @GetMapping("/department/{departmentId}")
    public ResponseEntity<Page<DoctorResponse>> getByDepartment(@PathVariable Long departmentId,
                                                                  Pageable pageable) {
        return ResponseEntity.ok(doctorService.getByDepartment(departmentId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DoctorResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(doctorService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DoctorResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody DoctorUpdateRequest request) {
        return ResponseEntity.ok(doctorService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        doctorService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<DoctorResponse> reactivate(@PathVariable Long id) {
        return ResponseEntity.ok(doctorService.reactivate(id));
    }
}
