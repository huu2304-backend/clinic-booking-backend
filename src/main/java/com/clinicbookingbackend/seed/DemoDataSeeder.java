package com.clinicbookingbackend.seed;

import com.clinicbookingbackend.entity.account.Account;
import com.clinicbookingbackend.entity.account.PatientProfile;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

// CBS-67: seed tài khoản/slot mẫu để demo/nghiệm thu nhanh, khỏi phải tạo tay qua Swagger mỗi
// lần chạy lại DB. Department + 50 Doctor (15 khoa) đã có sẵn từ migration V5 — seeder này chỉ
// bổ sung phần V5 CHƯA có: 1 Admin, vài Patient mẫu, và slot AVAILABLE cho "vài ngày tới" (ngày
// động theo Clock nên không thể là migration tĩnh, phải là CommandLineRunner chạy mỗi lần khởi
// động app để cửa sổ ngày luôn được đẩy tới).
//
// @Profile("!prod") — không chạy khi active profile là "prod" (tránh insert rác vào DB thật khi
// deploy, CBS-59..62 sẽ set SPRING_PROFILES_ACTIVE=prod). Không set profile (mặc định lúc dev
// local) hoặc set "dev"/"demo" đều chạy bình thường.
//
// Idempotent: mỗi lần chạy lại (mỗi lần app khởi động) không tạo trùng — existsBy... trước khi
// insert, cùng convention pre-check ở Service layer (xem CLAUDE.md "Standard CRUD pattern").
@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!prod")
public class DemoDataSeeder implements CommandLineRunner {

    static final String ADMIN_EMAIL = "admin@cbs.local";
    static final String ADMIN_PASSWORD = "Admin@123";

    static final String PATIENT_1_EMAIL = "benhnhan001@cbs.local";
    static final String PATIENT_2_EMAIL = "benhnhan002@cbs.local";
    static final String PATIENT_PASSWORD = "Patient@123";

    private static final int DOCTORS_PER_DEPARTMENT = 2;
    private static final int DAYS_AHEAD = 3;
    private static final int SLOT_DURATION_MINUTES = 30;
    private static final List<LocalTime> SLOT_START_TIMES =
            List.of(LocalTime.of(8, 0), LocalTime.of(8, 30), LocalTime.of(9, 0), LocalTime.of(9, 30));

    private final AccountRepository accountRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final DepartmentRepository departmentRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final DoctorScheduleRepository doctorScheduleRepository;
    private final AccountFactory accountFactory;
    private final Clock clock;

    @Override
    @Transactional
    public void run(String... args) {
        seedAdmin();
        seedPatient(PATIENT_1_EMAIL, "Nguyễn Văn Khách");
        seedPatient(PATIENT_2_EMAIL, "Trần Thị Khách");
        seedDoctorSchedules();
    }

    private void seedAdmin() {
        if (accountRepository.existsByEmail(ADMIN_EMAIL)) {
            return;
        }
        accountRepository.save(accountFactory.create(ADMIN_EMAIL, ADMIN_PASSWORD, Role.ADMIN));
        log.info("Seed: đã tạo tài khoản Admin demo {}", ADMIN_EMAIL);
    }

    private void seedPatient(String email, String fullName) {
        if (accountRepository.existsByEmail(email)) {
            return;
        }
        Account account = accountRepository.save(accountFactory.create(email, PATIENT_PASSWORD, Role.PATIENT));

        PatientProfile profile = new PatientProfile();
        profile.setAccount(account);
        profile.setFullName(fullName);
        patientProfileRepository.save(profile);
        log.info("Seed: đã tạo tài khoản Patient demo {}", email);
    }

    private void seedDoctorSchedules() {
        LocalDate today = LocalDate.now(clock);
        int createdCount = 0;

        for (Department department : departmentRepository.findAll()) {
            Page<DoctorProfile> doctors = doctorProfileRepository.findByDepartmentId(
                    department.getId(), PageRequest.of(0, DOCTORS_PER_DEPARTMENT, Sort.by("id")));

            for (DoctorProfile doctor : doctors) {
                createdCount += seedSlotsForDoctor(doctor, today);
            }
        }

        if (createdCount > 0) {
            log.info("Seed: đã tạo {} slot AVAILABLE mới cho {} ngày tới", createdCount, DAYS_AHEAD);
        }
    }

    private int seedSlotsForDoctor(DoctorProfile doctor, LocalDate today) {
        int createdCount = 0;
        for (int dayOffset = 1; dayOffset <= DAYS_AHEAD; dayOffset++) {
            LocalDate workDate = today.plusDays(dayOffset);
            for (LocalTime startTime : SLOT_START_TIMES) {
                if (doctorScheduleRepository.existsByDoctorProfileIdAndWorkDateAndStartTime(
                        doctor.getId(), workDate, startTime)) {
                    continue;
                }

                DoctorSchedule schedule = new DoctorSchedule();
                schedule.setDoctorProfile(doctor);
                schedule.setWorkDate(workDate);
                schedule.setStartTime(startTime);
                schedule.setEndTime(startTime.plusMinutes(SLOT_DURATION_MINUTES));
                schedule.setStatus(ScheduleStatus.AVAILABLE);
                doctorScheduleRepository.save(schedule);
                createdCount++;
            }
        }
        return createdCount;
    }
}
