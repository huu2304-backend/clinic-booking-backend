package com.clinicbookingbackend.seed;

import com.clinicbookingbackend.entity.account.Account;
import com.clinicbookingbackend.entity.account.enums.Role;
import com.clinicbookingbackend.entity.department.Department;
import com.clinicbookingbackend.entity.doctor.DoctorProfile;
import com.clinicbookingbackend.entity.doctorschedule.DoctorSchedule;
import com.clinicbookingbackend.entity.doctorschedule.enums.ScheduleStatus;
import com.clinicbookingbackend.repository.account.AccountRepository;
import com.clinicbookingbackend.repository.account.PatientProfileRepository;
import com.clinicbookingbackend.repository.department.DepartmentRepository;
import com.clinicbookingbackend.repository.doctor.DoctorProfileRepository;
import com.clinicbookingbackend.repository.doctorschedule.DoctorScheduleRepository;
import com.clinicbookingbackend.service.account.AccountFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemoDataSeederTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private PatientProfileRepository patientProfileRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private DoctorProfileRepository doctorProfileRepository;
    @Mock
    private DoctorScheduleRepository doctorScheduleRepository;
    @Mock
    private AccountFactory accountFactory;
    @Mock
    private Clock clock;

    @InjectMocks
    private DemoDataSeeder demoDataSeeder;

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 8);

    private void fixClockAtToday() {
        Instant instant = TODAY.atStartOfDay(ZONE).toInstant();
        when(clock.instant()).thenReturn(instant);
        when(clock.getZone()).thenReturn(ZONE);
    }

    // ---------- Admin/Patient seeding ----------

    @Test
    void run_shouldCreateAdminAndPatients_whenNotExisting() {
        when(accountRepository.existsByEmail(anyString())).thenReturn(false);
        when(accountFactory.create(anyString(), anyString(), any(Role.class)))
                .thenAnswer(inv -> {
                    Account account = new Account();
                    account.setEmail(inv.getArgument(0));
                    account.setRole(inv.getArgument(2));
                    return account;
                });
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> {
            Account account = inv.getArgument(0);
            account.setId(1L);
            return account;
        });
        when(departmentRepository.findAll()).thenReturn(List.of());
        fixClockAtToday();

        demoDataSeeder.run();

        verify(accountFactory).create(eq(DemoDataSeeder.ADMIN_EMAIL), eq(DemoDataSeeder.ADMIN_PASSWORD), eq(Role.ADMIN));
        verify(accountFactory).create(eq(DemoDataSeeder.PATIENT_1_EMAIL), eq(DemoDataSeeder.PATIENT_PASSWORD), eq(Role.PATIENT));
        verify(accountFactory).create(eq(DemoDataSeeder.PATIENT_2_EMAIL), eq(DemoDataSeeder.PATIENT_PASSWORD), eq(Role.PATIENT));
        verify(accountRepository, times(3)).save(any(Account.class));
        verify(patientProfileRepository, times(2)).save(any());
    }

    @Test
    void run_shouldSkipAdminAndPatients_whenAlreadyExisting() {
        when(accountRepository.existsByEmail(anyString())).thenReturn(true);
        when(departmentRepository.findAll()).thenReturn(List.of());
        fixClockAtToday();

        demoDataSeeder.run();

        verify(accountFactory, never()).create(anyString(), anyString(), any());
        verify(accountRepository, never()).save(any());
        verify(patientProfileRepository, never()).save(any());
    }

    // ---------- Doctor schedule seeding ----------

    @Test
    void run_shouldCreateScheduleSlots_forEveryMissingSlot() {
        when(accountRepository.existsByEmail(anyString())).thenReturn(true);

        Department department = new Department();
        department.setId(1L);
        when(departmentRepository.findAll()).thenReturn(List.of(department));

        DoctorProfile doctor = new DoctorProfile();
        doctor.setId(100L);
        when(doctorProfileRepository.findByDepartmentId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(doctor)));

        when(doctorScheduleRepository.existsByDoctorProfileIdAndWorkDateAndStartTime(
                anyLong(), any(LocalDate.class), any(LocalTime.class))).thenReturn(false);
        fixClockAtToday();

        demoDataSeeder.run();

        // 3 ngày tới x 4 slot/ngày = 12 slot cho 1 doctor
        ArgumentCaptor<DoctorSchedule> captor = ArgumentCaptor.forClass(DoctorSchedule.class);
        verify(doctorScheduleRepository, times(12)).save(captor.capture());

        DoctorSchedule firstSlot = captor.getAllValues().get(0);
        assertThat(firstSlot.getDoctorProfile()).isEqualTo(doctor);
        assertThat(firstSlot.getWorkDate()).isEqualTo(TODAY.plusDays(1));
        assertThat(firstSlot.getStartTime()).isEqualTo(LocalTime.of(8, 0));
        assertThat(firstSlot.getEndTime()).isEqualTo(LocalTime.of(8, 30));
        assertThat(firstSlot.getStatus()).isEqualTo(ScheduleStatus.AVAILABLE);
    }

    @Test
    void run_shouldSkipScheduleSlots_whenAlreadyExisting() {
        when(accountRepository.existsByEmail(anyString())).thenReturn(true);

        Department department = new Department();
        department.setId(1L);
        when(departmentRepository.findAll()).thenReturn(List.of(department));

        DoctorProfile doctor = new DoctorProfile();
        doctor.setId(100L);
        when(doctorProfileRepository.findByDepartmentId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(doctor)));

        when(doctorScheduleRepository.existsByDoctorProfileIdAndWorkDateAndStartTime(
                anyLong(), any(LocalDate.class), any(LocalTime.class))).thenReturn(true);
        fixClockAtToday();

        demoDataSeeder.run();

        verify(doctorScheduleRepository, never()).save(any());
    }

    @Test
    void run_shouldOnlyTakeFirstTwoDoctors_perDepartment() {
        when(accountRepository.existsByEmail(anyString())).thenReturn(true);

        Department department = new Department();
        department.setId(1L);
        when(departmentRepository.findAll()).thenReturn(List.of(department));

        // Page giả lập mock repository luôn tuân theo Pageable size — chỉ trả 2 doctor dù DB có nhiều hơn.
        when(doctorProfileRepository.findByDepartmentId(eq(1L), any(Pageable.class)))
                .thenAnswer(inv -> {
                    Pageable pageable = inv.getArgument(1);
                    DoctorProfile d1 = new DoctorProfile();
                    d1.setId(100L);
                    DoctorProfile d2 = new DoctorProfile();
                    d2.setId(101L);
                    List<DoctorProfile> all = List.of(d1, d2);
                    return new PageImpl<>(all.subList(0, Math.min(pageable.getPageSize(), all.size())));
                });
        when(doctorScheduleRepository.existsByDoctorProfileIdAndWorkDateAndStartTime(
                anyLong(), any(LocalDate.class), any(LocalTime.class))).thenReturn(false);
        fixClockAtToday();

        demoDataSeeder.run();

        // 2 doctor x 12 slot = 24
        verify(doctorScheduleRepository, times(24)).save(any());
    }
}
