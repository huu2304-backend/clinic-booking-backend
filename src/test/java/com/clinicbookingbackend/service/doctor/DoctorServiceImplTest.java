package com.clinicbookingbackend.service.doctor;

import com.clinicbookingbackend.common.exception.BusinessException;
import com.clinicbookingbackend.common.exception.ErrorCode;
import com.clinicbookingbackend.dto.doctor.DoctorCreateRequest;
import com.clinicbookingbackend.dto.doctor.DoctorResponse;
import com.clinicbookingbackend.dto.doctor.DoctorUpdateRequest;
import com.clinicbookingbackend.entity.account.Account;
import com.clinicbookingbackend.entity.account.enums.Role;
import com.clinicbookingbackend.entity.account.enums.Status;
import com.clinicbookingbackend.entity.department.Department;
import com.clinicbookingbackend.entity.doctor.DoctorProfile;
import com.clinicbookingbackend.mapper.doctor.DoctorMapper;
import com.clinicbookingbackend.repository.account.AccountRepository;
import com.clinicbookingbackend.repository.department.DepartmentRepository;
import com.clinicbookingbackend.repository.doctor.DoctorProfileRepository;
import com.clinicbookingbackend.service.account.AccountFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorServiceImplTest {

    @Mock
    private DoctorProfileRepository doctorProfileRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private DoctorMapper doctorMapper;

    @Mock
    private AccountFactory accountFactory;

    @InjectMocks
    private DoctorServiceImpl doctorService;

    private Department department;
    private Account account;
    private DoctorProfile profile;

    @BeforeEach
    void setUp() {
        department = new Department();
        department.setId(1L);
        department.setName("Nội tổng quát");

        account = new Account();
        account.setId(10L);
        account.setEmail("bacsi001@cbs.local");
        account.setRole(Role.DOCTOR);
        account.setStatus(Status.ACTIVE);

        profile = new DoctorProfile();
        profile.setId(100L);
        profile.setAccount(account);
        profile.setFullName("Nguyễn Văn An");
        profile.setDepartment(department);
    }

    @Test
    void create_shouldCreateAccountAndProfile_whenEmailNotUsedAndDepartmentExists() {
        DoctorCreateRequest request = new DoctorCreateRequest();
        request.setEmail("bacsi001@cbs.local");
        request.setPassword("Doctor@123");
        request.setFullName("Nguyễn Văn An");
        request.setDepartmentId(1L);

        DoctorResponse expectedResponse = new DoctorResponse(100L, "Nguyễn Văn An", "bacsi001@cbs.local", 1L, Status.ACTIVE, "Nội tổng quát");

        when(accountRepository.existsByEmail("bacsi001@cbs.local")).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(accountFactory.create("bacsi001@cbs.local", "Doctor@123", Role.DOCTOR)).thenReturn(account);
        when(accountRepository.save(account)).thenReturn(account);
        when(doctorProfileRepository.save(any(DoctorProfile.class))).thenReturn(profile);
        when(doctorMapper.toResponse(profile)).thenReturn(expectedResponse);

        DoctorResponse result = doctorService.create(request);

        assertThat(result).isEqualTo(expectedResponse);
        verify(accountFactory).create("bacsi001@cbs.local", "Doctor@123", Role.DOCTOR);
        verify(accountRepository).save(account);

        var profileCaptor = org.mockito.ArgumentCaptor.forClass(DoctorProfile.class);
        verify(doctorProfileRepository).save(profileCaptor.capture());
        assertThat(profileCaptor.getValue().getAccount()).isEqualTo(account);
        assertThat(profileCaptor.getValue().getDepartment()).isEqualTo(department);
    }

    @Test
    void create_shouldThrowConflict_whenEmailAlreadyExists() {
        DoctorCreateRequest request = new DoctorCreateRequest();
        request.setEmail("bacsi001@cbs.local");
        request.setPassword("Doctor@123");
        request.setFullName("Nguyễn Văn An");
        request.setDepartmentId(1L);

        when(accountRepository.existsByEmail("bacsi001@cbs.local")).thenReturn(true);

        assertThatThrownBy(() -> doctorService.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);

        verify(departmentRepository, never()).findById(any());
        verify(accountRepository, never()).save(any());
        verify(doctorProfileRepository, never()).save(any());
    }

    @Test
    void create_shouldThrowNotFound_whenDepartmentMissing() {
        DoctorCreateRequest request = new DoctorCreateRequest();
        request.setEmail("bacsi001@cbs.local");
        request.setPassword("Doctor@123");
        request.setFullName("Nguyễn Văn An");
        request.setDepartmentId(99L);

        when(accountRepository.existsByEmail("bacsi001@cbs.local")).thenReturn(false);
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> doctorService.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DEPARTMENT_NOT_FOUND);

        verify(accountRepository, never()).save(any());
        verify(doctorProfileRepository, never()).save(any());
    }

    @Test
    void update_shouldUpdateFullNameAndDepartment_whenFound() {
        DoctorUpdateRequest request = new DoctorUpdateRequest();
        request.setFullName("Nguyễn Văn B");
        request.setDepartmentId(2L);

        Department newDepartment = new Department();
        newDepartment.setId(2L);
        newDepartment.setName("Ngoại tổng quát");

        DoctorResponse expectedResponse = new DoctorResponse(100L, "Nguyễn Văn B", "bacsi001@cbs.local", 2L, Status.ACTIVE, "Ngoại tổng quát");

        when(doctorProfileRepository.findById(100L)).thenReturn(Optional.of(profile));
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(newDepartment));
        when(doctorProfileRepository.save(profile)).thenReturn(profile);
        when(doctorMapper.toResponse(profile)).thenReturn(expectedResponse);

        DoctorResponse result = doctorService.update(100L, request);

        assertThat(result).isEqualTo(expectedResponse);
        verify(doctorMapper).updateEntityFromRequest(request, profile);
        assertThat(profile.getDepartment()).isEqualTo(newDepartment);
    }

    @Test
    void update_shouldThrowNotFound_whenDoctorMissing() {
        DoctorUpdateRequest request = new DoctorUpdateRequest();
        request.setFullName("Nguyễn Văn B");
        request.setDepartmentId(2L);

        when(doctorProfileRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> doctorService.update(999L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DOCTOR_NOT_FOUND);

        verify(departmentRepository, never()).findById(any());
    }

    @Test
    void update_shouldThrowNotFound_whenNewDepartmentMissing() {
        DoctorUpdateRequest request = new DoctorUpdateRequest();
        request.setFullName("Nguyễn Văn B");
        request.setDepartmentId(99L);

        when(doctorProfileRepository.findById(100L)).thenReturn(Optional.of(profile));
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> doctorService.update(100L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DEPARTMENT_NOT_FOUND);

        verify(doctorProfileRepository, never()).save(any());
    }

    @Test
    void getById_shouldReturnResponse_whenFound() {
        DoctorResponse expectedResponse = new DoctorResponse(100L, "Nguyễn Văn An", "bacsi001@cbs.local", 1L, Status.ACTIVE, "Nội tổng quát");
        when(doctorProfileRepository.findById(100L)).thenReturn(Optional.of(profile));
        when(doctorMapper.toResponse(profile)).thenReturn(expectedResponse);

        DoctorResponse result = doctorService.getById(100L);

        assertThat(result).isEqualTo(expectedResponse);
    }

    @Test
    void getById_shouldThrowNotFound_whenMissing() {
        when(doctorProfileRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> doctorService.getById(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DOCTOR_NOT_FOUND);
    }

    @Test
    void getAll_shouldReturnMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        DoctorResponse expectedResponse = new DoctorResponse(100L, "Nguyễn Văn An", "bacsi001@cbs.local", 1L, Status.ACTIVE, "Nội tổng quát");

        when(doctorProfileRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(profile), pageable, 1));
        when(doctorMapper.toResponse(profile)).thenReturn(expectedResponse);

        var result = doctorService.getAll(pageable);

        assertThat(result.getContent()).containsExactly(expectedResponse);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getByDepartment_shouldReturnMappedPage_whenDepartmentExists() {
        Pageable pageable = PageRequest.of(0, 10);
        DoctorResponse expectedResponse = new DoctorResponse(100L, "Nguyễn Văn An", "bacsi001@cbs.local", 1L, Status.ACTIVE, "Nội tổng quát");

        when(departmentRepository.existsById(1L)).thenReturn(true);
        when(doctorProfileRepository.findByDepartmentId(1L, pageable)).thenReturn(new PageImpl<>(List.of(profile), pageable, 1));
        when(doctorMapper.toResponse(profile)).thenReturn(expectedResponse);

        var result = doctorService.getByDepartment(1L, pageable);

        assertThat(result.getContent()).containsExactly(expectedResponse);
    }

    @Test
    void getByDepartment_shouldThrowNotFound_whenDepartmentMissing() {
        Pageable pageable = PageRequest.of(0, 10);
        when(departmentRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> doctorService.getByDepartment(99L, pageable))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DEPARTMENT_NOT_FOUND);

        verify(doctorProfileRepository, never()).findByDepartmentId(any(), any());
    }

    @Test
    void delete_shouldDeactivateAccount_whenFound() {
        when(doctorProfileRepository.findById(100L)).thenReturn(Optional.of(profile));

        doctorService.delete(100L);

        assertThat(account.getStatus()).isEqualTo(Status.INACTIVE);
        verify(accountRepository).save(account);
        verify(doctorProfileRepository, never()).delete(any());
    }

    @Test
    void delete_shouldThrowNotFound_whenMissing() {
        when(doctorProfileRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> doctorService.delete(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DOCTOR_NOT_FOUND);

        verify(accountRepository, never()).save(any());
    }

    @Test
    void reactivate_shouldSetAccountActive_whenFound() {
        account.setStatus(Status.INACTIVE);
        DoctorResponse expectedResponse = new DoctorResponse(100L, "Nguyễn Văn An", "bacsi001@cbs.local", 1L, Status.ACTIVE, "Nội tổng quát");

        when(doctorProfileRepository.findById(100L)).thenReturn(Optional.of(profile));
        when(doctorMapper.toResponse(profile)).thenReturn(expectedResponse);

        DoctorResponse result = doctorService.reactivate(100L);

        assertThat(result).isEqualTo(expectedResponse);
        assertThat(account.getStatus()).isEqualTo(Status.ACTIVE);
        verify(accountRepository).save(account);
    }

    @Test
    void reactivate_shouldThrowNotFound_whenMissing() {
        when(doctorProfileRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> doctorService.reactivate(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DOCTOR_NOT_FOUND);

        verify(accountRepository, never()).save(any());
    }
}
