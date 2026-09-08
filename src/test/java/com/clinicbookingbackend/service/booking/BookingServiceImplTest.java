package com.clinicbookingbackend.service.booking;

import com.clinicbookingbackend.common.exception.BusinessException;
import com.clinicbookingbackend.common.exception.ErrorCode;
import com.clinicbookingbackend.dto.booking.AppointmentResponse;
import com.clinicbookingbackend.dto.booking.ConfirmAppointmentRequest;
import com.clinicbookingbackend.dto.booking.HoldResponse;
import com.clinicbookingbackend.entity.appointment.Appointment;
import com.clinicbookingbackend.entity.appointment.enums.AppointmentStatus;
import com.clinicbookingbackend.entity.doctor.DoctorProfile;
import com.clinicbookingbackend.entity.doctorschedule.DoctorSchedule;
import com.clinicbookingbackend.entity.doctorschedule.enums.ScheduleStatus;
import com.clinicbookingbackend.mapper.booking.AppointmentMapper;
import com.clinicbookingbackend.repository.appointment.AppointmentRepository;
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
    private Clock clock;

    private BookingServiceImpl bookingService;

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 7);
    private static final long TTL_MINUTES = 5;
    private static final long CANCELLATION_MIN_HOURS = 24;
    private static final Long PATIENT_ACCOUNT_ID = 1L;
    private static final Long OTHER_PATIENT_ACCOUNT_ID = 2L;

    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        bookingService = new BookingServiceImpl(
                doctorScheduleRepository, appointmentRepository, appointmentMapper, clock,
                TTL_MINUTES, CANCELLATION_MIN_HOURS);
    }

    private void fixClockAt(LocalTime time) {
        now = TODAY.atTime(time);
        Instant instant = now.atZone(ZONE).toInstant();
        when(clock.instant()).thenReturn(instant);
        when(clock.getZone()).thenReturn(ZONE);
    }

    private void fixClockAtDateTime(LocalDateTime dateTime) {
        now = dateTime;
        Instant instant = now.atZone(ZONE).toInstant();
        when(clock.instant()).thenReturn(instant);
        when(clock.getZone()).thenReturn(ZONE);
    }

    private Appointment confirmedAppointmentWith(Long patientAccountId, DoctorSchedule schedule) {
        Appointment appointment = new Appointment();
        appointment.setId(1L);
        appointment.setDoctorSchedule(schedule);
        appointment.setPatientAccountId(patientAccountId);
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        return appointment;
    }

    private Authentication authOf(Long accountId) {
        return new UsernamePasswordAuthenticationToken(
                accountId, null, List.of(new SimpleGrantedAuthority("ROLE_PATIENT")));
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

    // ---------- cancel() ----------
    // Appointment cố định workDate=2026-09-10, startTime=09:00 (từ scheduleWith) -> deadline hủy
    // (CANCELLATION_MIN_HOURS=24) là 2026-09-09T09:00.

    @Test
    void cancel_shouldSucceed_whenOwnerCancelsWithinDeadline() {
        DoctorSchedule schedule = scheduleWith(ScheduleStatus.BOOKED, null, null);
        Appointment appointment = confirmedAppointmentWith(PATIENT_ACCOUNT_ID, schedule);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(doctorScheduleRepository.save(any(DoctorSchedule.class))).thenAnswer(inv -> inv.getArgument(0));
        when(appointmentMapper.toResponse(any(Appointment.class))).thenReturn(
                new AppointmentResponse(1L, 1L, 100L, "Nguyễn Văn An",
                        schedule.getWorkDate(), schedule.getStartTime(), schedule.getEndTime(), "CANCELLED", LocalDateTime.now()));
        fixClockAtDateTime(LocalDateTime.of(2026, 9, 9, 8, 0));

        AppointmentResponse response = bookingService.cancel(1L, authOf(PATIENT_ACCOUNT_ID));

        assertThat(response.status()).isEqualTo("CANCELLED");
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(appointment.getCancelledAt()).isEqualTo(now);
        assertThat(schedule.getStatus()).isEqualTo(ScheduleStatus.AVAILABLE);
        verify(appointmentRepository).save(appointment);
        verify(doctorScheduleRepository).save(schedule);
    }

    @Test
    void cancel_shouldSucceed_whenExactlyAtDeadline() {
        DoctorSchedule schedule = scheduleWith(ScheduleStatus.BOOKED, null, null);
        Appointment appointment = confirmedAppointmentWith(PATIENT_ACCOUNT_ID, schedule);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(doctorScheduleRepository.save(any(DoctorSchedule.class))).thenAnswer(inv -> inv.getArgument(0));
        when(appointmentMapper.toResponse(any(Appointment.class))).thenReturn(
                new AppointmentResponse(1L, 1L, 100L, "Nguyễn Văn An",
                        schedule.getWorkDate(), schedule.getStartTime(), schedule.getEndTime(), "CANCELLED", LocalDateTime.now()));
        fixClockAtDateTime(LocalDateTime.of(2026, 9, 9, 9, 0));

        AppointmentResponse response = bookingService.cancel(1L, authOf(PATIENT_ACCOUNT_ID));

        assertThat(response.status()).isEqualTo("CANCELLED");
    }

    @Test
    void cancel_shouldReturn409_whenPastCancellationDeadline() {
        DoctorSchedule schedule = scheduleWith(ScheduleStatus.BOOKED, null, null);
        Appointment appointment = confirmedAppointmentWith(PATIENT_ACCOUNT_ID, schedule);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        fixClockAtDateTime(LocalDateTime.of(2026, 9, 9, 9, 1));

        assertThatThrownBy(() -> bookingService.cancel(1L, authOf(PATIENT_ACCOUNT_ID)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.CANCELLATION_NOT_ALLOWED));

        verify(appointmentRepository, never()).save(any());
        verify(doctorScheduleRepository, never()).save(any());
    }

    @Test
    void cancel_shouldReturn409_whenAlreadyCancelled() {
        DoctorSchedule schedule = scheduleWith(ScheduleStatus.AVAILABLE, null, null);
        Appointment appointment = confirmedAppointmentWith(PATIENT_ACCOUNT_ID, schedule);
        appointment.setStatus(AppointmentStatus.CANCELLED);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> bookingService.cancel(1L, authOf(PATIENT_ACCOUNT_ID)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.CANCELLATION_NOT_ALLOWED));

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void cancel_shouldReturn403_whenNotOwner() {
        DoctorSchedule schedule = scheduleWith(ScheduleStatus.BOOKED, null, null);
        Appointment appointment = confirmedAppointmentWith(OTHER_PATIENT_ACCOUNT_ID, schedule);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> bookingService.cancel(1L, authOf(PATIENT_ACCOUNT_ID)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void cancel_shouldReturn404_whenAppointmentNotFound() {
        when(appointmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.cancel(1L, authOf(PATIENT_ACCOUNT_ID)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.APPOINTMENT_NOT_FOUND));
    }
}
