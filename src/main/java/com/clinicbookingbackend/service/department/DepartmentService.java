package com.clinicbookingbackend.service.department;

import com.clinicbookingbackend.dto.department.DepartmentRequest;
import com.clinicbookingbackend.dto.department.DepartmentResponse;

import java.util.List;

public interface DepartmentService {

    DepartmentResponse create(DepartmentRequest request);

    DepartmentResponse update(Long id, DepartmentRequest request);

    DepartmentResponse getById(Long id);

    List<DepartmentResponse> getAll();

    void delete(Long id);
}
