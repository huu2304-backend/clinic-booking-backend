package com.clinicbookingbackend.service.booking;

import com.clinicbookingbackend.common.exception.BusinessException;
import com.clinicbookingbackend.common.exception.ErrorCode;
import com.clinicbookingbackend.dto.booking.AppointmentResponse;
import com.clinicbookingbackend.dto.booking.ConfirmAppointmentRequest;
import com.clinicbookingbackend.dto.booking.DoctorAppointmentResponse;
import com.clinicbookingbackend.dto.booking.HoldResponse;
import com.clinicbookingbackend.entity.account.Account;
import com.clinicbookingbackend.entity.account.PatientProfile;
import com.clinicbookingbackend.entity.appointment.Appointment;
import com.clinicbookingbackend.entity.appointment.enums.AppointmentStatus;
import com.clinicbookingbackend.entity.doctor.DoctorProfile;
import com.clinicbookingbackend.entity.doctorschedule.DoctorSchedule;
import com.clinicbookingbackend.entity.doctorschedule.enums.ScheduleStatus;
import com.clinicbookingbackend.mapper.booking.AppointmentMapper;
import com.clinicbookingbackend.mapper.booking.DoctorAppointmentMapper;
import com.clinicbookingbackend.repository.account.PatientProfileRepository;
import com.clinicbookingbackend.repository.appointment.AppointmentRepository;
import com.clinicbookingbackend.repository.doctor.DoctorProfileRepository;
import com.clinicbookingbackend.repository.doctorschedule.DoctorScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private DoctorScheduleRepository doctorScheduleRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private AppointmentMapper appointmentMapper;

    @Mock
    private DoctorProfileRepository doctorProfileRepository;

    @Mock
    private PatientProfileRepository patientProfileRepository;

    @Mock
    private DoctorAppointmentMapper doctorAppointmentMapper;

    @Mock
    private Clock clock;

    private BookingServiceImpl bookingService;

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 7);
    private static final long TTL_MINUTES = 5;
    private static final Long PATIENT_ACCOUNT_ID = 1L;
    private static final Long OTHER_PATIENT_ACCOUNT_ID = 2L;
    private static final Long DOCTOR_ACCOUNT_ID = 10L;

    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        bookingService = new BookingServiceImpl(
                doctorScheduleRepository, appointmentRepository, appointmentMapper,
                doctorProfileRepository, patientProfileRepository, doctorAppointmentMapper,
                clock, TTL_MINUTES);
    }

    private void fixClockAt(LocalTime time) {
        now = TODAY.atTime(time);
        Instant instant = now.atZone(ZONE).toInstant();
        when(clock.instant()).thenReturn(instant);
        when(clock.getZone()).thenReturn(ZONE);
    }

    private Authentication authOf(Long accountId) {
        return new UsernamePasswordAuthenticationToken(
                accountId, null, List.of(new SimpleGrantedAuthority("ROLE_PATIENT")));
    }

    private Authentication authOfDoctor(Long accountId) {
        return new UsernamePasswordAuthenticationToken(
                accountId, null, List.of(new SimpleGrantedAuthority("ROLE_DOCTOR")));
    }

    private Appointment confirmedAppointmentFor(Long patientAccountId, DoctorSchedule schedule) {
        Appointment appointment = new Appointment();
        appointment.setId(500L);
        appointment.setDoctorSchedule(schedule);
        appointment.setPatientAccountId(patientAccountId);
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        return appointment;
    }

    private DoctorSchedule scheduleWith(ScheduleStatus status, Long lockedByAccountId, LocalDateTime lockExpiresAt) {
        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setId(1L);
        schedule.setDoctorProfile(new DoctorProfile());
        schedule.setWorkDate(LocalDate.of(2026, 9, 10));
        schedule.setStartTime(LocalTime.of(9, 0));
        schedule.setEndTime(LocalTime.of(9, 30));
        schedule.setStatus(status);
        schedule.setLockedByAccountId(lockedByAccountId);
        schedule.setLockExpiresAt(lockExpiresAt);
        return schedule;
    }

    // ---------- hold() ----------

    @Test
    void hold_shouldSucceed_whenSlotIsAvailable() {
        DoctorSchedule schedule = scheduleWith(ScheduleStatus.AVAILABLE, null, null);
        when(doctorScheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(doctorScheduleRepository.save(any(DoctorSchedule.class))).thenAnswer(inv -> inv.getArgument(0));
        fixClockAt(LocalTime.of(8, 0));

        HoldResponse response = bookingService.hold(1L, authOf(PATIENT_ACCOUNT_ID));

        assertThat(response.doctorScheduleId()).isEqualTo(1L);
        assertThat(response.lockExpiresAt()).isEqualTo(now.plusMinutes(TTL_MINUTES));
        assertThat(schedule.getStatus()).isEqualTo(ScheduleStatus.LOCKED);
        assertThat(schedule.getLockedByAccountId()).isEqualTo(PATIENT_ACCOUNT_ID);
        verify(doctorScheduleRepository).save(schedule);
    }

    @Test
    void hold_shouldSucceed_whenSlotIsLockedButExpired() {
        fixClockAt(LocalTime.of(9, 0));
        DoctorSchedule schedule = scheduleWith(ScheduleStatus.LOCKED, OTHER_PATIENT_ACCOUNT_ID, now.minusMinutes(1));
        when(doctorScheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(doctorScheduleRepository.save(any(DoctorSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        HoldResponse response = bookingService.hold(1L, authOf(PATIENT_ACCOUNT_ID));

        assertThat(response.doctorScheduleId()).isEqualTo(1L);
        assertThat(schedule.getLockedByAccountId()).isEqualTo(PATIENT_ACCOUNT_ID);
    }

    @Test
    void hold_shouldReturn409_whenSlotIsLockedByOtherAndNotExpired() {
        fixClockAt(LocalTime.of(9, 0));
        DoctorSchedule schedule = scheduleWith(ScheduleStatus.LOCKED, OTHER_PATIENT_ACCOUNT_ID, now.plusMinutes(1));
        when(doctorScheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));

        assertThatThrownBy(() -> bookingService.hold(1L, authOf(PATIENT_ACCOUNT_ID)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.SLOT_UNAVAILABLE));

        verify(doctorScheduleRepository, never()).save(any());
    }

    @Test
    void hold_shouldReturn409_whenSlotIsBooked() {
        DoctorSchedule schedule = scheduleWith(ScheduleStatus.BOOKED, null, null);
        when(doctorScheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));

        assertThatThrownBy(() -> bookingService.hold(1L, authOf(PATIENT_ACCOUNT_ID)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.SLOT_UNAVAILABLE));

        verify(doctorScheduleRepository, never()).save(any());
    }

    @Test
    void hold_shouldReturn404_whenScheduleNotFound() {
        when(doctorScheduleRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.hold(1L, authOf(PATIENT_ACCOUNT_ID)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.SCHEDULE_NOT_FOUND));
    }

    // ---------- releaseHold() ----------

    @Test
    void releaseHold_shouldSucceed_whenOwnerReleases() {
        DoctorSchedule schedule = scheduleWith(ScheduleStatus.LOCKED, PATIENT_ACCOUNT_ID, LocalDateTime.now().plusMinutes(5));
        when(doctorScheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));

        bookingService.releaseHold(1L, authOf(PATIENT_ACCOUNT_ID));

        assertThat(schedule.getStatus()).isEqualTo(ScheduleStatus.AVAILABLE);
        assertThat(schedule.getLockedByAccountId()).isNull();
        verify(doctorScheduleRepository).save(schedule);
    }

    @Test
    void releaseHold_shouldBeNoop_whenNotOwner() {
        DoctorSchedule schedule = scheduleWith(ScheduleStatus.LOCKED, OTHER_PATIENT_ACCOUNT_ID, LocalDateTime.now().plusMinutes(5));
        when(doctorScheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));

        bookingService.releaseHold(1L, authOf(PATIENT_ACCOUNT_ID));

        assertThat(schedule.getStatus()).isEqualTo(ScheduleStatus.LOCKED);
        verify(doctorScheduleRepository, never()).save(any());
    }

    // ---------- confirm() ----------

    @Test
    void confirm_shouldSucceed_whenLockedBySelfAndNotExpired() {
        fixClockAt(LocalTime.of(9, 0));
        DoctorSchedule schedule = scheduleWith(ScheduleStatus.LOCKED, PATIENT_ACCOUNT_ID, now.plusMinutes(1));
        when(doctorScheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(doctorScheduleRepository.save(any(DoctorSchedule.class))).thenAnswer(inv -> inv.getArgument(0));
        when(appointmentRepository.existsOverlappingConfirmedAppointment(
                PATIENT_ACCOUNT_ID, schedule.getWorkDate(), schedule.getStartTime(), schedule.getEndTime()))
                .thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(appointmentMapper.toResponse(any(Appointment.class))).thenReturn(
                new AppointmentResponse(1L, 1L, 100L, "Nguyễn Văn An",
                        schedule.getWorkDate(), schedule.getStartTime(), schedule.getEndTime(), "CONFIRMED", now));

        AppointmentResponse response = bookingService.confirm(new ConfirmAppointmentRequest(1L), authOf(PATIENT_ACCOUNT_ID));

        assertThat(response.status()).isEqualTo("CONFIRMED");
        assertThat(schedule.getStatus()).isEqualTo(ScheduleStatus.BOOKED);
        assertThat(schedule.getLockedByAccountId()).isNull();
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    void confirm_shouldReturn400_whenScheduleNotLocked() {
        DoctorSchedule schedule = scheduleWith(ScheduleStatus.AVAILABLE, null, null);
        when(doctorScheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));

        assertThatThrownBy(() -> bookingService.confirm(new ConfirmAppointmentRequest(1L), authOf(PATIENT_ACCOUNT_ID)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.HOLD_REQUIRED));

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void confirm_shouldReturn409_whenLockedByOther() {
        fixClockAt(LocalTime.of(9, 0));
        DoctorSchedule schedule = scheduleWith(ScheduleStatus.LOCKED, OTHER_PATIENT_ACCOUNT_ID, now.plusMinutes(1));
        when(doctorScheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));

        assertThatThrownBy(() -> bookingService.confirm(new ConfirmAppointmentRequest(1L), authOf(PATIENT_ACCOUNT_ID)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.SLOT_UNAVAILABLE));

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void confirm_shouldReturn409_whenLockExpired() {
        fixClockAt(LocalTime.of(9, 0));
        DoctorSchedule schedule = scheduleWith(ScheduleStatus.LOCKED, PATIENT_ACCOUNT_ID, now.minusMinutes(1));
        when(doctorScheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));

        assertThatThrownBy(() -> bookingService.confirm(new ConfirmAppointmentRequest(1L), authOf(PATIENT_ACCOUNT_ID)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.SLOT_UNAVAILABLE));

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void confirm_shouldReturn409_whenPatientHasOverlappingConfirmedAppointment() {
        fixClockAt(LocalTime.of(9, 0));
        DoctorSchedule schedule = scheduleWith(ScheduleStatus.LOCKED, PATIENT_ACCOUNT_ID, now.plusMinutes(1));
        when(doctorScheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(appointmentRepository.existsOverlappingConfirmedAppointment(
                PATIENT_ACCOUNT_ID, schedule.getWorkDate(), schedule.getStartTime(), schedule.getEndTime()))
                .thenReturn(true);

        assertThatThrownBy(() -> bookingService.confirm(new ConfirmAppointmentRequest(1L), authOf(PATIENT_ACCOUNT_ID)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.PATIENT_SCHEDULE_CONFLICT));

        verify(appointmentRepository, never()).save(any());
        verify(doctorScheduleRepository, never()).save(any());
    }

    @Test
    void confirm_shouldReturn404_whenScheduleNotFound() {
        when(doctorScheduleRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.confirm(new ConfirmAppointmentRequest(1L), authOf(PATIENT_ACCOUNT_ID)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.SCHEDULE_NOT_FOUND));
    }

    // ---------- getMyAppointments() ----------

    @Test
    void getMyAppointments_shouldReturnAppointments_withPatientNamesResolved() {
        DoctorProfile doctorProfile = new DoctorProfile();
        doctorProfile.setId(100L);
        when(doctorProfileRepository.findByAccountId(DOCTOR_ACCOUNT_ID)).thenReturn(Optional.of(doctorProfile));

        DoctorSchedule schedule = scheduleWith(ScheduleStatus.BOOKED, null, null);
        Appointment appointment = confirmedAppointmentFor(PATIENT_ACCOUNT_ID, schedule);
        LocalDate date = LocalDate.of(2026, 9, 10);
        when(appointmentRepository.findByDoctorScheduleDoctorProfileIdAndDoctorScheduleWorkDateAndStatusOrderByDoctorScheduleStartTimeAsc(
                100L, date, AppointmentStatus.CONFIRMED)).thenReturn(List.of(appointment));

        Account patientAccount = new Account();
        patientAccount.setId(PATIENT_ACCOUNT_ID);
        PatientProfile patientProfile = new PatientProfile();
        patientProfile.setAccount(patientAccount);
        patientProfile.setFullName("Nguyễn Văn An");
        when(patientProfileRepository.findByAccountIdIn(List.of(PATIENT_ACCOUNT_ID))).thenReturn(List.of(patientProfile));

        DoctorAppointmentResponse expected = new DoctorAppointmentResponse(
                500L, schedule.getWorkDate(), schedule.getStartTime(), schedule.getEndTime(), "Nguyễn Văn An", "CONFIRMED");
        when(doctorAppointmentMapper.toResponse(appointment, "Nguyễn Văn An")).thenReturn(expected);

        List<DoctorAppointmentResponse> result = bookingService.getMyAppointments(date, authOfDoctor(DOCTOR_ACCOUNT_ID));

        assertThat(result).containsExactly(expected);
    }

    @Test
    void getMyAppointments_shouldReturn404_whenDoctorProfileNotFound() {
        when(doctorProfileRepository.findByAccountId(DOCTOR_ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.getMyAppointments(LocalDate.of(2026, 9, 10), authOfDoctor(DOCTOR_ACCOUNT_ID)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.DOCTOR_NOT_FOUND));

        verify(appointmentRepository, never())
                .findByDoctorScheduleDoctorProfileIdAndDoctorScheduleWorkDateAndStatusOrderByDoctorScheduleStartTimeAsc(
                        any(), any(), any());
    }

    @Test
    void getMyAppointments_shouldReturnEmptyList_andSkipPatientLookup_whenNoAppointments() {
        DoctorProfile doctorProfile = new DoctorProfile();
        doctorProfile.setId(100L);
        when(doctorProfileRepository.findByAccountId(DOCTOR_ACCOUNT_ID)).thenReturn(Optional.of(doctorProfile));

        LocalDate date = LocalDate.of(2026, 9, 10);
        when(appointmentRepository.findByDoctorScheduleDoctorProfileIdAndDoctorScheduleWorkDateAndStatusOrderByDoctorScheduleStartTimeAsc(
                100L, date, AppointmentStatus.CONFIRMED)).thenReturn(List.of());

        List<DoctorAppointmentResponse> result = bookingService.getMyAppointments(date, authOfDoctor(DOCTOR_ACCOUNT_ID));

        assertThat(result).isEmpty();
        verify(patientProfileRepository, never()).findByAccountIdIn(any());
    }
}
