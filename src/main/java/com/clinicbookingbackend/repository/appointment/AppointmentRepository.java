package com.clinicbookingbackend.repository.appointment;

import com.clinicbookingbackend.entity.appointment.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    Optional<Appointment> findByDoctorScheduleId(Long doctorScheduleId);

    // 1 Patient không được có 2 appointment CONFIRMED giao nhau về thời gian, kể cả với 2 bác sĩ
    // khác nhau — check nghiệp vụ ở tầng Service trước khi confirm (CBS-42), khác với UNIQUE
    // doctor_schedule_id ở DB (chỉ chặn trùng đúng 1 slot, không chặn trùng giờ khác slot).
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Appointment a " +
            "WHERE a.patientAccountId = :patientAccountId " +
            "AND a.status = com.clinicbookingbackend.entity.appointment.enums.AppointmentStatus.CONFIRMED " +
            "AND a.doctorSchedule.workDate = :workDate " +
            "AND a.doctorSchedule.startTime < :endTime AND :startTime < a.doctorSchedule.endTime")
    boolean existsOverlappingConfirmedAppointment(@Param("patientAccountId") Long patientAccountId,
                                                   @Param("workDate") LocalDate workDate,
                                                   @Param("startTime") LocalTime startTime,
                                                   @Param("endTime") LocalTime endTime);
}
