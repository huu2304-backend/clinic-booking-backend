package com.clinicbookingbackend.entity.account;

import com.clinicbookingbackend.entity.account.enums.Gender;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "patient_profile")
public class PatientProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Quan hệ 1-1 với Account, bắt buộc không được null và unique ở mức DB
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private Account account;

    @Column(nullable = false)
    private String fullName;

    // Kiểu DATE trong DB map chuẩn xác nhất với LocalDate trong Java
    @Column(nullable = true)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = true)
    private Gender gender;

    @Column(length = 20, nullable = true)
    private String phoneNumber;
}