package com.clinicbookingbackend.service.account;

import com.clinicbookingbackend.common.exception.BusinessException;
import com.clinicbookingbackend.common.exception.ErrorCode;
import com.clinicbookingbackend.dto.account.PatientProfileResponse;
import com.clinicbookingbackend.dto.account.PatientProfileUpdateRequest;
import com.clinicbookingbackend.entity.account.PatientProfile;
import com.clinicbookingbackend.mapper.account.PatientProfileMapper;
import com.clinicbookingbackend.repository.account.PatientProfileRepository;
import com.clinicbookingbackend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientProfileServiceImpl implements PatientProfileService {

    private final PatientProfileRepository patientProfileRepository;
    private final PatientProfileMapper patientProfileMapper;

    @Override
    @Transactional(readOnly = true)
    public PatientProfileResponse getMyProfile(Authentication authentication) {
        PatientProfile profile = findByCurrentAccount(authentication);
        return patientProfileMapper.toResponse(profile);
    }

    @Override
    @Transactional
    public PatientProfileResponse updateMyProfile(PatientProfileUpdateRequest request, Authentication authentication) {
        PatientProfile profile = findByCurrentAccount(authentication);
        log.info("Patient accountId={} cập nhật hồ sơ cá nhân", profile.getAccount().getId());

        patientProfileMapper.updateEntityFromRequest(request, profile);
        PatientProfile saved = patientProfileRepository.save(profile);

        log.info("Cập nhật hồ sơ thành công, patientProfileId={}", saved.getId());
        return patientProfileMapper.toResponse(saved);
    }

    private PatientProfile findByCurrentAccount(Authentication authentication) {
        Long accountId = SecurityUtils.getCurrentAccountId(authentication);
        return patientProfileRepository.findByAccountId(accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy hồ sơ Patient"));
    }
}
