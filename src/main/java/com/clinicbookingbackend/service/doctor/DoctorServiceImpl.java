package com.clinicbookingbackend.service.doctor;

import com.clinicbookingbackend.common.exception.BusinessException;
import com.clinicbookingbackend.common.exception.ErrorCode;
import com.clinicbookingbackend.dto.doctor.DoctorCreateRequest;
import com.clinicbookingbackend.dto.doctor.DoctorResponse;
import com.clinicbookingbackend.dto.doctor.DoctorUpdateRequest;
import com.clinicbookingbackend.entity.account.Account;
import com.clinicbookingbackend.entity.account.enums.Role;
import com.clinicbookingbackend.entity.account.enums.Status;
import com.clinicbookingbackend.entity.department.Department;
import com.clinicbookingbackend.entity.doctor.DoctorProfile;
import com.clinicbookingbackend.mapper.doctor.DoctorMapper;
import com.clinicbookingbackend.repository.account.AccountRepository;
import com.clinicbookingbackend.repository.department.DepartmentRepository;
import com.clinicbookingbackend.repository.doctor.DoctorProfileRepository;
import com.clinicbookingbackend.service.account.AccountFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DoctorServiceImpl implements DoctorService {

    private final DoctorProfileRepository doctorProfileRepository;
    private final AccountRepository accountRepository;
    private final DepartmentRepository departmentRepository;
    private final DoctorMapper doctorMapper;
    private final AccountFactory accountFactory;

    @Override
    @Transactional
    public DoctorResponse create(DoctorCreateRequest request) {
        String email = request.getEmail();
        log.info("Bắt đầu tạo bác sĩ mới: {}", email);

        // Guard 1: email đã tồn tại chưa (dùng chung AccountRepository với AuthService)
        if (accountRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS,
                    "Email '" + email + "' đã được sử dụng");
        }

        // Guard 2: department phải tồn tại trước khi gắn — tránh vi phạm FK not-null ở tầng DB
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPARTMENT_NOT_FOUND,
                        "Không tìm thấy khoa với id=" + request.getDepartmentId()));

        // Tạo Account trước (role DOCTOR, không tự đăng ký được — chỉ Admin tạo qua API này)
        Account account = accountFactory.create(email, request.getPassword(), Role.DOCTOR);
        Account savedAccount = accountRepository.save(account);

        // Rồi tạo DoctorProfile gắn với Account + Department vừa fetch
        DoctorProfile profile = new DoctorProfile();
        profile.setAccount(savedAccount);
        profile.setFullName(request.getFullName());
        profile.setDepartment(department);
        DoctorProfile savedProfile = doctorProfileRepository.save(profile);

        log.info("Tạo bác sĩ thành công, id={}, accountId={}", savedProfile.getId(), savedAccount.getId());
        return doctorMapper.toResponse(savedProfile);
    }

    @Override
    @Transactional
    public DoctorResponse update(Long id, DoctorUpdateRequest request) {
        log.info("Bắt đầu sửa thông tin bác sĩ id={}", id);

        DoctorProfile profile = doctorProfileRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCTOR_NOT_FOUND,
                        "Không tìm thấy bác sĩ với id=" + id));

        // Cho phép "chuyển khoa" — fetch lại department mới theo departmentId trong request
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPARTMENT_NOT_FOUND,
                        "Không tìm thấy khoa với id=" + request.getDepartmentId()));

        doctorMapper.updateEntityFromRequest(request, profile);
        profile.setDepartment(department);

        DoctorProfile saved = doctorProfileRepository.save(profile);
        log.info("Sửa bác sĩ thành công, id={}", saved.getId());

        return doctorMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorResponse getById(Long id) {
        DoctorProfile profile = doctorProfileRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCTOR_NOT_FOUND,
                        "Không tìm thấy bác sĩ với id=" + id));
        return doctorMapper.toResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DoctorResponse> getAll(Pageable pageable) {
        return doctorProfileRepository.findAll(pageable)
                .map(doctorMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DoctorResponse> getByDepartment(Long departmentId, Pageable pageable) {
        if (!departmentRepository.existsById(departmentId)) {
            throw new BusinessException(ErrorCode.DEPARTMENT_NOT_FOUND,
                    "Không tìm thấy khoa với id=" + departmentId);
        }
        return doctorProfileRepository.findByDepartmentId(departmentId, pageable)
                .map(doctorMapper::toResponse);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        DoctorProfile profile = doctorProfileRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCTOR_NOT_FOUND,
                        "Không tìm thấy bác sĩ với id=" + id));

        // Soft-delete: không xóa row nào, chỉ deactivate Account -> doctor không login được nữa
        // (AuthService.login đã check status ACTIVE), nhưng vẫn giữ lịch sử tham chiếu.
        Account account = profile.getAccount();
        account.setStatus(Status.INACTIVE);
        accountRepository.save(account);

        log.info("Vô hiệu hóa bác sĩ thành công, id={}, accountId={}", id, account.getId());
    }

    @Override
    @Transactional
    public DoctorResponse reactivate(Long id) {
        // Đường thoát cho hệ quả của soft-delete: email của doctor đã INACTIVE vẫn giữ
        // unique trên account, nên account đó không bao giờ tái sử dụng được nếu không
        // có cách kích hoạt lại (tạo mới sẽ luôn bị chặn bởi EMAIL_ALREADY_EXISTS).
        DoctorProfile profile = doctorProfileRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCTOR_NOT_FOUND,
                        "Không tìm thấy bác sĩ với id=" + id));

        Account account = profile.getAccount();
        account.setStatus(Status.ACTIVE);
        accountRepository.save(account);

        log.info("Kích hoạt lại bác sĩ thành công, id={}, accountId={}", id, account.getId());
        return doctorMapper.toResponse(profile);
    }
}
