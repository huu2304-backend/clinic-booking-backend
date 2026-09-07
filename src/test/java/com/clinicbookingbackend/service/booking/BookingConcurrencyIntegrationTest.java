package com.clinicbookingbackend.service.booking;

import com.clinicbookingbackend.common.exception.BusinessException;
import com.clinicbookingbackend.common.exception.ErrorCode;
import com.clinicbookingbackend.dto.booking.AppointmentResponse;
import com.clinicbookingbackend.dto.booking.ConfirmAppointmentRequest;
import com.clinicbookingbackend.dto.booking.HoldResponse;
import com.clinicbookingbackend.entity.account.Account;
import com.clinicbookingbackend.entity.account.enums.Role;
import com.clinicbookingbackend.entity.account.enums.Status;
import com.clinicbookingbackend.entity.appointment.Appointment;
import com.clinicbookingbackend.entity.doctor.DoctorProfile;
import com.clinicbookingbackend.entity.doctorschedule.DoctorSchedule;
import com.clinicbookingbackend.entity.doctorschedule.enums.ScheduleStatus;
import com.clinicbookingbackend.repository.account.AccountRepository;
import com.clinicbookingbackend.repository.appointment.AppointmentRepository;
import com.clinicbookingbackend.repository.doctor.DoctorProfileRepository;
import com.clinicbookingbackend.repository.doctorschedule.DoctorScheduleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// CBS-43: test giả lập concurrent request thật (nhiều thread, DB Postgres thật qua Flyway) ở cả
// 2 tầng hold (CBS-72) và confirm (CBS-42) — bổ sung cho BookingServiceImplTest (Mockito, không
// verify được race condition thật ở tầng DB/@Version). Cần Postgres đang chạy (docker compose up
// -d postgres) để chạy được test này — xem README mục 13/CLAUDE.md.
@SpringBootTest
class BookingConcurrencyIntegrationTest {

    private static final int PATIENT_COUNT = 8;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private DoctorScheduleRepository doctorScheduleRepository;

    @Autowired
    private DoctorProfileRepository doctorProfileRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    private DoctorProfile doctorProfile;
    private final List<Account> testPatientAccounts = new ArrayList<>();
    private final List<Long> testScheduleIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        doctorProfile = doctorProfileRepository.findAll(PageRequest.of(0, 1)).getContent().get(0);

        for (int i = 0; i < PATIENT_COUNT; i++) {
            Account account = new Account();
            account.setEmail("cbs43-concurrency-test-patient-" + i + "-" + System.nanoTime() + "@test.local");
            account.setPasswordHash("N/A");
            account.setRole(Role.PATIENT);
            account.setStatus(Status.ACTIVE);
            testPatientAccounts.add(accountRepository.save(account));
        }
    }

    @AfterEach
    void tearDown() {
        // Dọn sạch dữ liệu test khỏi DB dev dùng chung (không phải DB riêng cho test) — không để
        // lại rác ảnh hưởng các session/test khác.
        for (Long scheduleId : testScheduleIds) {
            appointmentRepository.findByDoctorScheduleId(scheduleId).ifPresent(appointmentRepository::delete);
        }
        doctorScheduleRepository.deleteAllById(testScheduleIds);
        accountRepository.deleteAll(testPatientAccounts);
    }

    private DoctorSchedule newAvailableSchedule(LocalDate workDate, LocalTime startTime) {
        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setDoctorProfile(doctorProfile);
        schedule.setWorkDate(workDate);
        schedule.setStartTime(startTime);
        schedule.setEndTime(startTime.plusMinutes(30));
        schedule.setStatus(ScheduleStatus.AVAILABLE);
        DoctorSchedule saved = doctorScheduleRepository.save(schedule);
        testScheduleIds.add(saved.getId());
        return saved;
    }

    private Authentication authOf(Long accountId) {
        return new UsernamePasswordAuthenticationToken(
                accountId, null, List.of(new SimpleGrantedAuthority("ROLE_PATIENT")));
    }

    @Test
    void hold_shouldAllowExactlyOneWinner_whenNPatientsRaceForSameSlot() throws Exception {
        DoctorSchedule schedule = newAvailableSchedule(LocalDate.of(2099, 1, 7), LocalTime.of(9, 0));

        List<Callable<HoldResponse>> tasks = new ArrayList<>();
        CountDownLatch startLatch = new CountDownLatch(1);
        for (Account patient : testPatientAccounts) {
            tasks.add(() -> {
                startLatch.await();
                return bookingService.hold(schedule.getId(), authOf(patient.getId()));
            });
        }

        ExecutorService executor = Executors.newFixedThreadPool(PATIENT_COUNT);
        try {
            List<Future<HoldResponse>> futures = new ArrayList<>();
            for (Callable<HoldResponse> task : tasks) {
                futures.add(executor.submit(task));
            }
            startLatch.countDown();

            AtomicInteger successCount = new AtomicInteger();
            AtomicInteger conflictCount = new AtomicInteger();
            for (Future<HoldResponse> future : futures) {
                try {
                    future.get(10, TimeUnit.SECONDS);
                    successCount.incrementAndGet();
                } catch (Exception ex) {
                    assertIsSlotConflict(ex.getCause());
                    conflictCount.incrementAndGet();
                }
            }

            assertThat(successCount.get()).isEqualTo(1);
            assertThat(conflictCount.get()).isEqualTo(PATIENT_COUNT - 1);
        } finally {
            executor.shutdownNow();
        }

        DoctorSchedule reloaded = doctorScheduleRepository.findById(schedule.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(ScheduleStatus.LOCKED);
    }

    @Test
    void confirm_shouldAllowOnlyLockOwner_whenMultiplePatientsRaceToConfirm() throws Exception {
        DoctorSchedule schedule = newAvailableSchedule(LocalDate.of(2099, 1, 8), LocalTime.of(9, 0));
        Account owner = testPatientAccounts.get(0);
        bookingService.hold(schedule.getId(), authOf(owner.getId()));

        List<Callable<AppointmentResponse>> tasks = new ArrayList<>();
        CountDownLatch startLatch = new CountDownLatch(1);
        for (Account patient : testPatientAccounts) {
            tasks.add(() -> {
                startLatch.await();
                return bookingService.confirm(new ConfirmAppointmentRequest(schedule.getId()), authOf(patient.getId()));
            });
        }

        ExecutorService executor = Executors.newFixedThreadPool(PATIENT_COUNT);
        try {
            List<Future<AppointmentResponse>> futures = new ArrayList<>();
            for (Callable<AppointmentResponse> task : tasks) {
                futures.add(executor.submit(task));
            }
            startLatch.countDown();

            int successCount = 0;
            int rejectedCount = 0;
            for (Future<AppointmentResponse> future : futures) {
                try {
                    future.get(10, TimeUnit.SECONDS);
                    successCount++;
                } catch (Exception ex) {
                    assertThat(ex.getCause()).isInstanceOf(BusinessException.class);
                    rejectedCount++;
                }
            }

            assertThat(successCount).isEqualTo(1);
            assertThat(rejectedCount).isEqualTo(PATIENT_COUNT - 1);
        } finally {
            executor.shutdownNow();
        }

        Optional<Appointment> appointment = appointmentRepository.findByDoctorScheduleId(schedule.getId());
        assertThat(appointment).isPresent();
        assertThat(appointment.get().getPatientAccountId()).isEqualTo(owner.getId());
    }

    @Test
    void confirm_shouldReturn409AndNotCreateAppointment_whenLockAlreadyExpired() {
        DoctorSchedule schedule = newAvailableSchedule(LocalDate.of(2099, 1, 9), LocalTime.of(9, 0));
        Account patient = testPatientAccounts.get(0);

        schedule.setStatus(ScheduleStatus.LOCKED);
        schedule.setLockedByAccountId(patient.getId());
        schedule.setLockExpiresAt(LocalDateTime.now().minusMinutes(1));
        doctorScheduleRepository.save(schedule);

        assertThatThrownBy(() -> bookingService.confirm(new ConfirmAppointmentRequest(schedule.getId()), authOf(patient.getId())))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.SLOT_UNAVAILABLE));

        assertThat(appointmentRepository.findByDoctorScheduleId(schedule.getId())).isEmpty();
    }

    // Gọi thẳng BookingService (không qua HTTP) nên GlobalExceptionHandler không có cơ hội
    // convert ObjectOptimisticLockingFailureException -> BusinessException(SLOT_UNAVAILABLE)
    // như khi chạy qua controller thật — cả 2 đều là "thua cuộc tranh chấp slot" hợp lệ ở đây.
    private void assertIsSlotConflict(Throwable cause) {
        if (cause instanceof BusinessException businessException) {
            assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.SLOT_UNAVAILABLE);
        } else {
            assertThat(cause).isInstanceOf(ObjectOptimisticLockingFailureException.class);
        }
    }
}
