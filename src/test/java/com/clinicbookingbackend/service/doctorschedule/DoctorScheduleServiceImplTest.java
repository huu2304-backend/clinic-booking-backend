package com.clinicbookingbackend.service.doctorschedule;

import com.clinicbookingbackend.common.exception.BusinessException;
import com.clinicbookingbackend.common.exception.ErrorCode;
import com.clinicbookingbackend.dto.doctorschedule.DoctorScheduleCreateRequest;
import com.clinicbookingbackend.dto.doctorschedule.DoctorScheduleResponse;
import com.clinicbookingbackend.dto.doctorschedule.DoctorScheduleUpdateRequest;
import com.clinicbookingbackend.entity.account.Account;
import com.clinicbookingbackend.entity.department.Department;
import com.clinicbookingbackend.entity.doctor.DoctorProfile;
import com.clinicbookingbackend.entity.doctorschedule.DoctorSchedule;
import com.clinicbookingbackend.entity.doctorschedule.enums.ScheduleStatus;
import com.clinicbookingbackend.mapper.doctorschedule.DoctorScheduleMapper;
import com.clinicbookingbackend.repository.doctor.DoctorProfileRepository;
import com.clinicbookingbackend.repository.doctorschedule.DoctorScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorScheduleServiceImplTest {

    @Mock
    private DoctorScheduleRepository doctorScheduleRepository;

    @Mock
    private DoctorProfileRepository doctorProfileRepository;

    @Mock
    private DoctorScheduleMapper doctorScheduleMapper;

    @InjectMocks
    private DoctorScheduleServiceImpl doctorScheduleService;

    private DoctorProfile doctorProfile;
    private DoctorProfile otherDoctorProfile;

    private static final Long DOCTOR_ACCOUNT_ID = 10L;
    private static final Long OTHER_DOCTOR_ACCOUNT_ID = 20L;

    @BeforeEach
    void setUp() {
        Department department = new Department();
        department.setId(1L);
        department.setName("Nội tổng quát");

        Account account = new Account();
        account.setId(DOCTOR_ACCOUNT_ID);

        doctorProfile = new DoctorProfile();
        doctorProfile.setId(100L);
        doctorProfile.setAccount(account);
        doctorProfile.setFullName("Nguyễn Văn An");
        doctorProfile.setDepartment(department);

        Account otherAccount = new Account();
        otherAccount.setId(OTHER_DOCTOR_ACCOUNT_ID);

        otherDoctorProfile = new DoctorProfile();
        otherDoctorProfile.setId(200L);
        otherDoctorProfile.setAccount(otherAccount);
        otherDoctorProfile.setFullName("Trần Thị Bình");
        otherDoctorProfile.setDepartment(department);
    }

    private Authentication authOf(Long accountId, String role) {
        return new UsernamePasswordAuthenticationToken(
                accountId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }

    private DoctorScheduleCreateRequest createRequest(Long doctorId) {
        return new DoctorScheduleCreateRequest(doctorId, LocalDate.of(2026, 9, 10),
                LocalTime.of(9, 0), LocalTime.of(9, 30));
    }

    @Test
    void create_shouldSucceed_whenDoctorCreatesOwnSlot() {
        when(doctorProfileRepository.findById(100L)).thenReturn(Optional.of(doctorProfile));
        when(doctorScheduleRepository.existsByDoctorProfileIdAndWorkDateAndStartTime(100L, LocalDate.of(2026, 9, 10), LocalTime.of(9, 0)))
                .thenReturn(false);
        when(doctorScheduleRepository.save(any(DoctorSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(doctorScheduleMapper.toResponse(any(DoctorSchedule.class))).thenReturn(
                new DoctorScheduleResponse(1L, 100L, "Nguyễn Văn An", LocalDate.of(2026, 9, 10), LocalTime.of(9, 0), LocalTime.of(9, 30), ScheduleStatus.AVAILABLE));

        DoctorScheduleResponse response = doctorScheduleService.create(createRequest(100L), authOf(DOCTOR_ACCOUNT_ID, "DOCTOR"));

        assertThat(response.status()).isEqualTo(ScheduleStatus.AVAILABLE);
        verify(doctorScheduleRepository).save(any(DoctorSchedule.class));
    }

    @Test
    void create_shouldReturn403_whenDoctorCreatesSlotForAnotherDoctor() {
        when(doctorProfileRepository.findById(200L)).thenReturn(Optional.of(otherDoctorProfile));

        assertThatThrownBy(() -> doctorScheduleService.create(createRequest(200L), authOf(DOCTOR_ACCOUNT_ID, "DOCTOR")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));

        verify(doctorScheduleRepository, never()).save(any());
    }

    @Test
    void create_shouldSucceed_whenAdminCreatesSlotForAnyDoctor() {
        when(doctorProfileRepository.findById(200L)).thenReturn(Optional.of(otherDoctorProfile));
        when(doctorScheduleRepository.existsByDoctorProfileIdAndWorkDateAndStartTime(200L, LocalDate.of(2026, 9, 10), LocalTime.of(9, 0)))
                .thenReturn(false);
        when(doctorScheduleRepository.save(any(DoctorSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(doctorScheduleMapper.toResponse(any(DoctorSchedule.class))).thenReturn(
                new DoctorScheduleResponse(1L, 200L, "Trần Thị Bình", LocalDate.of(2026, 9, 10), LocalTime.of(9, 0), LocalTime.of(9, 30), ScheduleStatus.AVAILABLE));

        DoctorScheduleResponse response = doctorScheduleService.create(createRequest(200L), authOf(999L, "ADMIN"));

        assertThat(response.doctorId()).isEqualTo(200L);
        verify(doctorScheduleRepository).save(any(DoctorSchedule.class));
    }

    @Test
    void create_shouldReturn409_whenSlotAlreadyExists() {
        when(doctorProfileRepository.findById(100L)).thenReturn(Optional.of(doctorProfile));
        when(doctorScheduleRepository.existsByDoctorProfileIdAndWorkDateAndStartTime(100L, LocalDate.of(2026, 9, 10), LocalTime.of(9, 0)))
                .thenReturn(true);

        assertThatThrownBy(() -> doctorScheduleService.create(createRequest(100L), authOf(DOCTOR_ACCOUNT_ID, "DOCTOR")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.SCHEDULE_SLOT_ALREADY_EXISTS));

        verify(doctorScheduleRepository, never()).save(any());
    }

    @Test
    void create_shouldReturn400_whenStartTimeIsAfterEndTime() {
        when(doctorProfileRepository.findById(100L)).thenReturn(Optional.of(doctorProfile));
        DoctorScheduleCreateRequest invalidRequest = new DoctorScheduleCreateRequest(
                100L, LocalDate.of(2026, 9, 10), LocalTime.of(10, 0), LocalTime.of(9, 0));

        assertThatThrownBy(() -> doctorScheduleService.create(invalidRequest, authOf(DOCTOR_ACCOUNT_ID, "DOCTOR")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_TIME_RANGE));
    }

    @Test
    void update_shouldReturn409_whenScheduleIsBooked() {
        DoctorSchedule bookedSchedule = new DoctorSchedule();
        bookedSchedule.setId(1L);
        bookedSchedule.setDoctorProfile(doctorProfile);
        bookedSchedule.setStatus(ScheduleStatus.BOOKED);
        when(doctorScheduleRepository.findById(1L)).thenReturn(Optional.of(bookedSchedule));

        DoctorScheduleUpdateRequest updateRequest = new DoctorScheduleUpdateRequest(
                LocalDate.of(2026, 9, 11), LocalTime.of(9, 0), LocalTime.of(9, 30));

        assertThatThrownBy(() -> doctorScheduleService.update(1L, updateRequest, authOf(DOCTOR_ACCOUNT_ID, "DOCTOR")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.SCHEDULE_MODIFICATION_NOT_ALLOWED));

        verify(doctorScheduleRepository, never()).save(any());
    }

    @Test
    void delete_shouldReturn409_whenScheduleIsBooked() {
        DoctorSchedule bookedSchedule = new DoctorSchedule();
        bookedSchedule.setId(1L);
        bookedSchedule.setDoctorProfile(doctorProfile);
        bookedSchedule.setStatus(ScheduleStatus.BOOKED);
        when(doctorScheduleRepository.findById(1L)).thenReturn(Optional.of(bookedSchedule));

        assertThatThrownBy(() -> doctorScheduleService.delete(1L, authOf(DOCTOR_ACCOUNT_ID, "DOCTOR")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.SCHEDULE_MODIFICATION_NOT_ALLOWED));

        verify(doctorScheduleRepository, never()).delete(any());
    }

    @Test
    void delete_shouldSucceed_whenScheduleIsAvailable() {
        DoctorSchedule availableSchedule = new DoctorSchedule();
        availableSchedule.setId(1L);
        availableSchedule.setDoctorProfile(doctorProfile);
        availableSchedule.setStatus(ScheduleStatus.AVAILABLE);
        when(doctorScheduleRepository.findById(1L)).thenReturn(Optional.of(availableSchedule));

        doctorScheduleService.delete(1L, authOf(DOCTOR_ACCOUNT_ID, "DOCTOR"));

        verify(doctorScheduleRepository).delete(availableSchedule);
    }
}
