package com.clinicbookingbackend.entity.doctor;

import com.clinicbookingbackend.entity.account.Account;
import com.clinicbookingbackend.entity.department.Department;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "doctor_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DoctorProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private Account account;
    @Column(nullable = false)
    private String fullName;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;
}
