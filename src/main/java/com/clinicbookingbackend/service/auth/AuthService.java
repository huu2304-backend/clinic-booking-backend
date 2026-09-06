package com.clinicbookingbackend.service.auth;

import com.clinicbookingbackend.common.exception.BusinessException;
import com.clinicbookingbackend.common.exception.ErrorCode;
import com.clinicbookingbackend.dto.auth.LoginRequest;
import com.clinicbookingbackend.dto.auth.LoginResponse;
import com.clinicbookingbackend.dto.auth.RegisterRequest;
import com.clinicbookingbackend.dto.auth.RegisterResponse;
import com.clinicbookingbackend.entity.account.Account;
import com.clinicbookingbackend.entity.account.PatientProfile;
import com.clinicbookingbackend.entity.account.enums.Role;
import com.clinicbookingbackend.entity.account.enums.Status;
import com.clinicbookingbackend.repository.account.AccountRepository;
import com.clinicbookingbackend.repository.account.PatientProfileRepository;
import com.clinicbookingbackend.repository.doctor.DoctorProfileRepository;
import com.clinicbookingbackend.security.JwtUtil;
import com.clinicbookingbackend.service.account.AccountFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AccountRepository accountRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AccountFactory accountFactory;

    @Transactional
    public RegisterResponse registerPatient(RegisterRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();
        log.info("Bắt đầu xử lý đăng ký tài khoản cho email: {}", normalizedEmail);

        if (accountRepository.existsByEmail(normalizedEmail)) {
            log.warn("Đăng ký thất bại: Email {} đã tồn tại", normalizedEmail);
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        Account account = accountFactory.create(normalizedEmail, request.getPassword(), Role.PATIENT);
        Account savedAccount = accountRepository.save(account);

        PatientProfile profile = new PatientProfile();
        profile.setAccount(savedAccount);
        profile.setFullName(request.getFullName().trim());
        profile.setDateOfBirth(request.getDateOfBirth());
        profile.setPhoneNumber(request.getPhoneNumber());
        profile.setGender(request.getGender());
        patientProfileRepository.save(profile);

        log.info("Đăng ký thành công tài khoản ID: {}", savedAccount.getId());

        return new RegisterResponse(
                savedAccount.getId(),
                savedAccount.getEmail(),
                profile.getFullName(),
                savedAccount.getRole().name());
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Account account = accountRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (account.getStatus() != Status.ACTIVE) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }

        String fullName = resolveFullName(account);

        String token = jwtUtil.generateToken(account.getId(), account.getRole());

        return new LoginResponse(
                token,
                account.getId(),
                account.getEmail(),
                fullName,
                account.getRole().name()
        );
    }

    // fullName nằm ở bảng profile khác nhau tùy role (PatientProfile/DoctorProfile);
    // Admin chưa có bảng profile riêng trong schema hiện tại nên tạm dùng email.
    private String resolveFullName(Account account) {
        return switch (account.getRole()) {
            case PATIENT -> patientProfileRepository.findByAccountId(account.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy hồ sơ Patient"))
                    .getFullName();
            case DOCTOR -> doctorProfileRepository.findByAccountId(account.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy hồ sơ Doctor"))
                    .getFullName();
            case ADMIN -> account.getEmail();
        };
    }
}