package com.clinicbookingbackend.service.account;

import com.clinicbookingbackend.dto.account.PatientProfileResponse;
import com.clinicbookingbackend.dto.account.PatientProfileUpdateRequest;
import org.springframework.security.core.Authentication;

public interface PatientProfileService {

    // CBS-66: accountId lấy từ JWT (SecurityUtils), không nhận id từ query/param.
    PatientProfileResponse getMyProfile(Authentication authentication);

    // CBS-66: chỉ sửa fullName/dateOfBirth/gender/phoneNumber/address — không có field dữ liệu
    // y tế (BR-ACC-05, dữ liệu y tế nằm ở TriageSession/Appointment).
    PatientProfileResponse updateMyProfile(PatientProfileUpdateRequest request, Authentication authentication);
}
