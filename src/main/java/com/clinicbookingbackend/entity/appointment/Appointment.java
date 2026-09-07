package com.clinicbookingbackend.entity.appointment;

import com.clinicbookingbackend.entity.appointment.enums.AppointmentStatus;
import com.clinicbookingbackend.entity.doctorschedule.DoctorSchedule;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // UNIQUE(doctor_schedule_id) ở DB (V8) — lưới an toàn cuối chống trùng lịch (BR-APT-01).
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_schedule_id", nullable = false, unique = true)
    private DoctorSchedule doctorSchedule;

    // accountId thô của Patient — theo đúng thiết kế patient_account_id FK->account.id,
    // không cần fetch cả entity Account chỉ để gắn lịch hẹn.
    @Column(name = "patient_account_id", nullable = false)
    private Long patientAccountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentStatus status = AppointmentStatus.CONFIRMED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
