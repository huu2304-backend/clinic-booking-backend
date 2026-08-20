package com.clinicbookingbackend.service.auth;

import com.clinicbookingbackend.common.exception.BusinessException;
import com.clinicbookingbackend.common.exception.ErrorCode;
import com.clinicbookingbackend.dto.auth.RegisterRequest;
import com.clinicbookingbackend.dto.auth.RegisterResponse;
import com.clinicbookingbackend.entity.account.Account;
import com.clinicbookingbackend.entity.account.PatientProfile;
import com.clinicbookingbackend.entity.account.enums.Role;
import com.clinicbookingbackend.entity.account.enums.Status;
import com.clinicbookingbackend.repository.AccountRepository;
import com.clinicbookingbackend.repository.PatientProfileRepository;
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
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public RegisterResponse registerPatient(RegisterRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();
        log.info("Bắt đầu xử lý đăng ký tài khoản cho email: {}", normalizedEmail);

        if (accountRepository.existsByEmail(normalizedEmail)) {
            log.warn("Đăng ký thất bại: Email {} đã tồn tại", normalizedEmail);
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        Account account = new Account();
        account.setEmail(normalizedEmail);
        account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        account.setRole(Role.PATIENT);
        account.setStatus(Status.ACTIVE);
        Account savedAccount = accountRepository.save(account);

        PatientProfile profile = new PatientProfile();
        profile.setAccount(savedAccount);
        profile.setFullName(request.getFullName().trim());
        profile.setDateOfBirth(request.getDateOfBirth());
        profile.setPhoneNumber(request.getPhoneNumber());
        profile.setGender(request.getGender()); // đã là enum, không cần parse tay
        patientProfileRepository.save(profile);

        log.info("Đăng ký thành công tài khoản ID: {}", savedAccount.getId());

        return new RegisterResponse(
                savedAccount.getId(),
                savedAccount.getEmail(),
                profile.getFullName(),
                savedAccount.getRole().name());
    }
}