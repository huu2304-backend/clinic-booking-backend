package com.clinicbookingbackend.repository.doctorschedule;

import com.clinicbookingbackend.entity.doctorschedule.DoctorSchedule;
import com.clinicbookingbackend.entity.doctorschedule.enums.ScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    // Job quét slot LOCKED quá TTL (CBS-52) — 1 UPDATE...WHERE nguyên tử ở tầng DB, không phải
    // đọc-rồi-ghi ở tầng app nên không cần qua Optimistic Locking; vẫn tự tăng version để các
    // bản ghi entity cũ trong Persistence Context (nếu có) phát hiện đúng đã bị đổi.
    @Modifying
    @Query("UPDATE DoctorSchedule s SET s.status = com.clinicbookingbackend.entity.doctorschedule.enums.ScheduleStatus.AVAILABLE, " +
            "s.lockedByAccountId = null, s.lockExpiresAt = null, s.version = s.version + 1 " +
            "WHERE s.status = com.clinicbookingbackend.entity.doctorschedule.enums.ScheduleStatus.LOCKED " +
            "AND s.lockExpiresAt < :now")
    int expireStaleLocks(@Param("now") LocalDateTime now);
}
