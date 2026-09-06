package com.clinicbookingbackend.service.department;

import com.clinicbookingbackend.common.exception.BusinessException;
import com.clinicbookingbackend.common.exception.ErrorCode;
import com.clinicbookingbackend.dto.department.DepartmentRequest;
import com.clinicbookingbackend.dto.department.DepartmentResponse;
import com.clinicbookingbackend.entity.department.Department;
import com.clinicbookingbackend.mapper.department.DepartmentMapper;
import com.clinicbookingbackend.repository.department.DepartmentRepository;
import com.clinicbookingbackend.repository.doctor.DoctorProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceImplTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private DepartmentMapper departmentMapper;

    @Mock
    private DoctorProfileRepository doctorProfileRepository;

    @InjectMocks
    private DepartmentServiceImpl departmentService;

    private DepartmentRequest request;
    private Department department;

    @BeforeEach
    void setUp() {
        request = new DepartmentRequest();
        request.setName("Nội tổng quát");
        request.setDescription("Khám nội tổng quát");

        department = new Department();
        department.setId(1L);
        department.setName("Nội tổng quát");
        department.setDescription("Khám nội tổng quát");
    }

    @Test
    void create_shouldSaveAndReturnResponse_whenNameNotDuplicated() {
        DepartmentResponse expectedResponse = new DepartmentResponse(1L, "Nội tổng quát", "Khám nội tổng quát");
        when(departmentRepository.existsByName("Nội tổng quát")).thenReturn(false);
        when(departmentMapper.toEntity(request)).thenReturn(department);
        when(departmentRepository.save(department)).thenReturn(department);
        when(departmentMapper.toResponse(department)).thenReturn(expectedResponse);

        DepartmentResponse result = departmentService.create(request);

        assertThat(result).isEqualTo(expectedResponse);
        verify(departmentRepository).save(department);
    }

    @Test
    void create_shouldThrowConflict_whenNameAlreadyExists() {
        when(departmentRepository.existsByName("Nội tổng quát")).thenReturn(true);

        assertThatThrownBy(() -> departmentService.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DEPARTMENT_NAME_ALREADY_EXISTS);

        verify(departmentRepository, never()).save(any());
    }

    @Test
    void update_shouldUpdateAndReturnResponse_whenFound() {
        DepartmentResponse expectedResponse = new DepartmentResponse(1L, "Nội tổng quát", "Mô tả mới");
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(departmentRepository.existsByNameAndIdNot("Nội tổng quát", 1L)).thenReturn(false);
        when(departmentRepository.save(department)).thenReturn(department);
        when(departmentMapper.toResponse(department)).thenReturn(expectedResponse);

        DepartmentResponse result = departmentService.update(1L, request);

        assertThat(result).isEqualTo(expectedResponse);
        verify(departmentMapper).updateEntityFromRequest(request, department);
        verify(departmentRepository).save(department);
    }

    @Test
    void update_shouldThrowNotFound_whenDepartmentDoesNotExist() {
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.update(99L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DEPARTMENT_NOT_FOUND);

        verify(departmentRepository, never()).save(any());
    }

    @Test
    void update_shouldThrowConflict_whenNameUsedByAnotherDepartment() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(departmentRepository.existsByNameAndIdNot("Nội tổng quát", 1L)).thenReturn(true);

        assertThatThrownBy(() -> departmentService.update(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DEPARTMENT_NAME_ALREADY_EXISTS);

        verify(departmentRepository, never()).save(any());
    }

    @Test
    void getById_shouldReturnResponse_whenFound() {
        DepartmentResponse expectedResponse = new DepartmentResponse(1L, "Nội tổng quát", "Khám nội tổng quát");
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(departmentMapper.toResponse(department)).thenReturn(expectedResponse);

        DepartmentResponse result = departmentService.getById(1L);

        assertThat(result).isEqualTo(expectedResponse);
    }

    @Test
    void getById_shouldThrowNotFound_whenMissing() {
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.getById(99L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DEPARTMENT_NOT_FOUND);
    }

    @Test
    void getAll_shouldReturnMappedList() {
        Department department2 = new Department();
        department2.setId(2L);
        department2.setName("Ngoại tổng quát");

        DepartmentResponse response1 = new DepartmentResponse(1L, "Nội tổng quát", "Khám nội tổng quát");
        DepartmentResponse response2 = new DepartmentResponse(2L, "Ngoại tổng quát", null);

        when(departmentRepository.findAll()).thenReturn(List.of(department, department2));
        when(departmentMapper.toResponse(department)).thenReturn(response1);
        when(departmentMapper.toResponse(department2)).thenReturn(response2);

        List<DepartmentResponse> result = departmentService.getAll();

        assertThat(result).containsExactly(response1, response2);
    }

    @Test
    void delete_shouldRemoveDepartment_whenNoDoctorsAssigned() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(doctorProfileRepository.existsByDepartmentId(1L)).thenReturn(false);

        departmentService.delete(1L);

        verify(departmentRepository, times(1)).delete(department);
    }

    @Test
    void delete_shouldThrowNotFound_whenMissing() {
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.delete(99L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DEPARTMENT_NOT_FOUND);

        verify(departmentRepository, never()).delete(any());
    }

    @Test
    void delete_shouldThrowConflict_whenAnyDoctorStillAssigned() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(doctorProfileRepository.existsByDepartmentId(1L)).thenReturn(true);

        assertThatThrownBy(() -> departmentService.delete(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DEPARTMENT_HAS_DOCTORS);

        verify(departmentRepository, never()).delete(any());
    }
}
