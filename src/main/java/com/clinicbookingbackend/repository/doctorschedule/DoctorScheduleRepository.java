package com.clinicbookingbackend.repository.doctorschedule;

import com.clinicbookingbackend.entity.doctorschedule.DoctorSchedule;
import com.clinicbookingbackend.entity.doctorschedule.enums.ScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, Long> {

    // Pre-check trùng slot khi tạo mới — UNIQUE thật vẫn nằm ở DB (uq_doctor_schedule_slot),
    // check này chỉ để trả lỗi nghiệp vụ rõ nghĩa thay vì để lộ DataIntegrityViolationException.
    boolean existsByDoctorProfileIdAndWorkDateAndStartTime(Long doctorProfileId, LocalDate workDate, LocalTime startTime);

    // Cùng mục đích nhưng dùng khi sửa — loại trừ chính bản thân record đang sửa.
    boolean existsByDoctorProfileIdAndWorkDateAndStartTimeAndIdNot(Long doctorProfileId, LocalDate workDate, LocalTime startTime, Long id);

    // Dùng cho patient xem slot trống trong ngày — lọc theo status + cận giờ, khác 2 method
    // existsBy trên (chỉ dùng pre-check khi tạo/sửa, không lọc).
    List<DoctorSchedule> findByDoctorProfileIdAndWorkDateAndStatusAndStartTimeGreaterThanEqualOrderByStartTimeAsc(
            Long doctorProfileId, LocalDate workDate, ScheduleStatus status, LocalTime startTimeFrom);
}
