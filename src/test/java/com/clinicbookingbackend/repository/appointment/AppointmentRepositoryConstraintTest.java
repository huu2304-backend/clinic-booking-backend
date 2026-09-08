package com.clinicbookingbackend.repository.appointment;

import com.clinicbookingbackend.entity.account.Account;
import com.clinicbookingbackend.entity.account.enums.Role;
import com.clinicbookingbackend.entity.account.enums.Status;
import com.clinicbookingbackend.entity.appointment.Appointment;
import com.clinicbookingbackend.entity.department.Department;
import com.clinicbookingbackend.entity.doctor.DoctorProfile;
import com.clinicbookingbackend.entity.doctorschedule.DoctorSchedule;
import com.clinicbookingbackend.entity.doctorschedule.enums.ScheduleStatus;
import com.clinicbookingbackend.repository.account.AccountRepository;
import com.clinicbookingbackend.repository.department.DepartmentRepository;
import com.clinicbookingbackend.repository.doctor.DoctorProfileRepository;
import com.clinicbookingbackend.repository.doctorschedule.DoctorScheduleRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// US-BOOK-01.1: chung minh BR-APT-01 (UNIQUE doctor_schedule_id) va BR-SCH-01 (CHECK status)
// thuc su nam o tang DB, doc lap voi check nghiep vu o BookingServiceImpl - insert/update thang
// qua repository/EntityManager, bo qua toan bo service layer. Can Postgres dang chay (docker
// compose up -d postgres), giong BookingConcurrencyIntegrationTest.
@SpringBootTest
class AppointmentRepositoryConstraintTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private DoctorProfileRepository doctorProfileRepository;

    @Autowired
    private DoctorScheduleRepository doctorScheduleRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @PersistenceContext
    private EntityManager entityManager;

    private Account patientAccount;
    private Account doctorAccount;
    private Department department;
    private DoctorProfile doctorProfile;
    private DoctorSchedule doctorSchedule;

    @BeforeEach
    void setUp() {
        String suffix = System.nanoTime() + "";

        patientAccount = accountRepository.save(newAccount("apt-constraint-patient-" + suffix, Role.PATIENT));
        doctorAccount = accountRepository.save(newAccount("apt-constraint-doctor-" + suffix, Role.DOCTOR));

        department = new Department();
        department.setName("Apt Constraint Test " + suffix);
        department = departmentRepository.save(department);

        doctorProfile = new DoctorProfile();
        doctorProfile.setAccount(doctorAccount);
        doctorProfile.setFullName("Bac Si Test Constraint");
        doctorProfile.setDepartment(department);
        doctorProfile = doctorProfileRepository.save(doctorProfile);

        doctorSchedule = new DoctorSchedule();
        doctorSchedule.setDoctorProfile(doctorProfile);
        doctorSchedule.setWorkDate(LocalDate.of(2099, 6, 15));
        doctorSchedule.setStartTime(LocalTime.of(9, 0));
        doctorSchedule.setEndTime(LocalTime.of(9, 30));
        doctorSchedule.setStatus(ScheduleStatus.AVAILABLE);
        doctorSchedule = doctorScheduleRepository.save(doctorSchedule);
    }

    @AfterEach
    void tearDown() {
        appointmentRepository.findByDoctorScheduleId(doctorSchedule.getId()).ifPresent(appointmentRepository::delete);
        doctorScheduleRepository.deleteById(doctorSchedule.getId());
        doctorProfileRepository.deleteById(doctorProfile.getId());
        departmentRepository.deleteById(department.getId());
        accountRepository.deleteById(doctorAccount.getId());
        accountRepository.deleteById(patientAccount.getId());
    }

    @Test
    void insertingSecondAppointment_shouldViolateDbUniqueConstraint_evenBypassingServiceLayer() {
        appointmentRepository.saveAndFlush(newAppointment(doctorSchedule, patientAccount.getId()));

        Appointment second = newAppointment(doctorSchedule, patientAccount.getId());

        assertThatThrownBy(() -> appointmentRepository.saveAndFlush(second))
                .isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(ex -> assertThat(rootCauseMessage(ex)).contains("uq_appointment_doctor_schedule"));
    }

    @Test
    void updatingStatusToInvalidValue_shouldViolateDbCheckConstraint() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> transactionTemplate.execute(status -> {
            entityManager.createNativeQuery("UPDATE doctor_schedule SET status = 'FOO' WHERE id = :id")
                    .setParameter("id", doctorSchedule.getId())
                    .executeUpdate();
            return null;
        })).satisfies(ex -> assertThat(rootCauseMessage(ex)).contains("chk_doctor_schedule_status"));
    }

    private String rootCauseMessage(Throwable ex) {
        Throwable root = ex;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getMessage();
    }

    private Account newAccount(String emailPrefix, Role role) {
        Account account = new Account();
        account.setEmail(emailPrefix + "@test.local");
        account.setPasswordHash("N/A");
        account.setRole(role);
        account.setStatus(Status.ACTIVE);
        return account;
    }

    private Appointment newAppointment(DoctorSchedule schedule, Long patientAccountId) {
        Appointment appointment = new Appointment();
        appointment.setDoctorSchedule(schedule);
        appointment.setPatientAccountId(patientAccountId);
        return appointment;
    }
}
