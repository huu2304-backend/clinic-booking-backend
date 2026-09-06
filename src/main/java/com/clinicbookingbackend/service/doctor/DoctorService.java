package com.clinicbookingbackend.service.doctor;

import com.clinicbookingbackend.dto.doctor.DoctorCreateRequest;
import com.clinicbookingbackend.dto.doctor.DoctorResponse;
import com.clinicbookingbackend.dto.doctor.DoctorUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DoctorService {

    DoctorResponse create(DoctorCreateRequest request);

    DoctorResponse update(Long id, DoctorUpdateRequest request);

    DoctorResponse getById(Long id);

    Page<DoctorResponse> getAll(Pageable pageable);

    Page<DoctorResponse> getByDepartment(Long departmentId, Pageable pageable);

    void delete(Long id);

    DoctorResponse reactivate(Long id);
}
