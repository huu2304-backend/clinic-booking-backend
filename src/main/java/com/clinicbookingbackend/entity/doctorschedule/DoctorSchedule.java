package com.clinicbookingbackend.entity.doctorschedule;

import com.clinicbookingbackend.entity.doctor.DoctorProfile;
import com.clinicbookingbackend.entity.doctorschedule.enums.ScheduleStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "doctor_schedule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DoctorSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_profile_id", nullable = false)
    private DoctorProfile doctorProfile;

    @Column(nullable = false)
    private LocalDate workDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScheduleStatus status = ScheduleStatus.AVAILABLE;

    // accountId thô của Patient đang giữ chỗ — chỉ dùng để so khớp chủ sở hữu (CBS-72),
    // không cần fetch cả entity Account.
    @Column(name = "locked_by_account_id")
    private Long lockedByAccountId;

    // Mốc thời gian hết hạn giữ chỗ (TTL, BR-SCH-05) — so sánh qua LocalDateTime.now(clock),
    // cùng convention với LocalDate.now(clock)/LocalTime.now(clock) ở DoctorScheduleServiceImpl.
    @Column(name = "lock_expires_at")
    private LocalDateTime lockExpiresAt;

    // Optimistic Locking (BR-APT-02): 2 request cùng transition AVAILABLE/LOCKED-hết-hạn -> LOCKED
    // chỉ 1 UPDATE thành công, request còn lại ném ObjectOptimisticLockingFailureException.
    @Version
    @Column(nullable = false)
    private Long version;
}
