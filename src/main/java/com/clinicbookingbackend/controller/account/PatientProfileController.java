package com.clinicbookingbackend.controller.account;

import com.clinicbookingbackend.dto.account.PatientProfileResponse;
import com.clinicbookingbackend.dto.account.PatientProfileUpdateRequest;
import com.clinicbookingbackend.service.account.PatientProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// CBS-66: Patient xem/sửa hồ sơ cá nhân của chính mình — accountId luôn lấy từ JWT
// (Authentication), không có path variable {id} nên không thể đụng hồ sơ người khác.
@RestController
@RequestMapping("/api/patients/me")
@RequiredArgsConstructor
public class PatientProfileController {

    private final PatientProfileService patientProfileService;

    @GetMapping
    public ResponseEntity<PatientProfileResponse> getMyProfile(Authentication authentication) {
        return ResponseEntity.ok(patientProfileService.getMyProfile(authentication));
    }

    @PutMapping
    public ResponseEntity<PatientProfileResponse> updateMyProfile(@Valid @RequestBody PatientProfileUpdateRequest request,
                                                                    Authentication authentication) {
        return ResponseEntity.ok(patientProfileService.updateMyProfile(request, authentication));
    }
}
