package com.clinicbookingbackend.scheduler;

import com.clinicbookingbackend.entity.account.Account;
import com.clinicbookingbackend.entity.account.enums.Role;
import com.clinicbookingbackend.entity.account.enums.Status;
import com.clinicbookingbackend.entity.doctor.DoctorProfile;
import com.clinicbookingbackend.entity.doctorschedule.DoctorSchedule;
import com.clinicbookingbackend.entity.doctorschedule.enums.ScheduleStatus;
import com.clinicbookingbackend.repository.account.AccountRepository;
import com.clinicbookingbackend.repository.doctor.DoctorProfileRepository;
import com.clinicbookingbackend.repository.doctorschedule.DoctorScheduleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

// CBS-52 AC: "Có test verify slot LOCKED quá TTL sẽ được job trả về AVAILABLE" — gọi thẳng
// expireStaleLocks() (không chờ @Scheduled interval thật) trên DB Postgres thật, cùng cách tiếp
// cận với BookingConcurrencyIntegrationTest (CBS-43).
@SpringBootTest
class SlotLockExpirySweepJobTest {

    @Autowired
    private SlotLockExpirySweepJob sweepJob;

    @Autowired
    private DoctorScheduleRepository doctorScheduleRepository;

    @Autowired
    private DoctorProfileRepository doctorProfileRepository;

    @Autowired
    private AccountRepository accountRepository;

    private Long scheduleId;
    private Account patientAccount;

    @AfterEach
    void tearDown() {
        if (scheduleId != null) {
            doctorScheduleRepository.deleteById(scheduleId);
        }
        if (patientAccount != null) {
            accountRepository.delete(patientAccount);
        }
    }

    @Test
    void expireStaleLocks_shouldReturnSlotToAvailable_whenLockExpired() {
        DoctorProfile doctorProfile = doctorProfileRepository.findAll(PageRequest.of(0, 1)).getContent().get(0);

        Account account = new Account();
        account.setEmail("cbs52-sweep-job-test-" + System.nanoTime() + "@test.local");
        account.setPasswordHash("N/A");
        account.setRole(Role.PATIENT);
        account.setStatus(Status.ACTIVE);
        patientAccount = accountRepository.save(account);

        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setDoctorProfile(doctorProfile);
        schedule.setWorkDate(LocalDate.of(2099, 2, 1));
        schedule.setStartTime(LocalTime.of(9, 0));
        schedule.setEndTime(LocalTime.of(9, 30));
        schedule.setStatus(ScheduleStatus.LOCKED);
        schedule.setLockedByAccountId(patientAccount.getId());
        schedule.setLockExpiresAt(LocalDateTime.now().minusMinutes(1));
        scheduleId = doctorScheduleRepository.save(schedule).getId();

        sweepJob.expireStaleLocks();

        DoctorSchedule reloaded = doctorScheduleRepository.findById(scheduleId).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(ScheduleStatus.AVAILABLE);
        assertThat(reloaded.getLockedByAccountId()).isNull();
        assertThat(reloaded.getLockExpiresAt()).isNull();
    }

    @Test
    void expireStaleLocks_shouldNotTouchSlot_whenLockNotYetExpired() {
        DoctorProfile doctorProfile = doctorProfileRepository.findAll(PageRequest.of(0, 1)).getContent().get(0);

        Account account = new Account();
        account.setEmail("cbs52-sweep-job-test-" + System.nanoTime() + "@test.local");
        account.setPasswordHash("N/A");
        account.setRole(Role.PATIENT);
        account.setStatus(Status.ACTIVE);
        patientAccount = accountRepository.save(account);

        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setDoctorProfile(doctorProfile);
        schedule.setWorkDate(LocalDate.of(2099, 2, 2));
        schedule.setStartTime(LocalTime.of(9, 0));
        schedule.setEndTime(LocalTime.of(9, 30));
        schedule.setStatus(ScheduleStatus.LOCKED);
        schedule.setLockedByAccountId(patientAccount.getId());
        schedule.setLockExpiresAt(LocalDateTime.now().plusMinutes(5));
        scheduleId = doctorScheduleRepository.save(schedule).getId();

        sweepJob.expireStaleLocks();

        DoctorSchedule reloaded = doctorScheduleRepository.findById(scheduleId).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(ScheduleStatus.LOCKED);
        assertThat(reloaded.getLockedByAccountId()).isEqualTo(patientAccount.getId());
    }
}
