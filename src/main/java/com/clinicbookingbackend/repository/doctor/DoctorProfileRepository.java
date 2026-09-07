package com.clinicbookingbackend.repository.doctor;

import com.clinicbookingbackend.entity.account.enums.Status;
import com.clinicbookingbackend.entity.doctor.DoctorProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, Long> {
    boolean existsByAccountId(Long accountId);

    // Dùng cho guard "chặn xóa khoa còn bác sĩ" — check bất kể status (kể cả doctor đã
    // soft-delete/INACTIVE), vì doctor_profile.department_id là FK NOT NULL không có
    // ON DELETE: DB sẽ chặn xóa department bất kể status của doctor, nên guard ở tầng
    // app phải khớp đúng với điều đó để trả lỗi DEPARTMENT_HAS_DOCTORS rõ ràng, thay vì
    // để lọt xuống DataIntegrityViolationException chung chung không thể tự khắc phục.
    boolean existsByDepartmentId(Long departmentId);

    @EntityGraph(attributePaths = {"account", "department"})
    @Override
    Optional<DoctorProfile> findById(Long id);

    @EntityGraph(attributePaths = {"account", "department"})
    @Override
    Page<DoctorProfile> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"account", "department"})
    Page<DoctorProfile> findByDepartmentId(Long departmentId, Pageable pageable);

    // Dùng cho patient xem danh sách bác sĩ theo khoa — chỉ ACTIVE, khác findByDepartmentId
    // (admin, thấy cả INACTIVE để quản lý).
    @EntityGraph(attributePaths = {"account", "department"})
    Page<DoctorProfile> findByDepartmentIdAndAccountStatus(Long departmentId, Status status, Pageable pageable);

    Optional<DoctorProfile> findByAccountId(Long accountId);
}
