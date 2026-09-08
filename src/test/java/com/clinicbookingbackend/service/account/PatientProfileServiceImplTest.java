package com.clinicbookingbackend.service.account;

import com.clinicbookingbackend.common.exception.BusinessException;
import com.clinicbookingbackend.common.exception.ErrorCode;
import com.clinicbookingbackend.dto.account.PatientProfileResponse;
import com.clinicbookingbackend.dto.account.PatientProfileUpdateRequest;
import com.clinicbookingbackend.entity.account.Account;
import com.clinicbookingbackend.entity.account.PatientProfile;
import com.clinicbookingbackend.entity.account.enums.Gender;
import com.clinicbookingbackend.mapper.account.PatientProfileMapper;
import com.clinicbookingbackend.repository.account.PatientProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientProfileServiceImplTest {

    @Mock
    private PatientProfileRepository patientProfileRepository;

    @Mock
    private PatientProfileMapper patientProfileMapper;

    @InjectMocks
    private PatientProfileServiceImpl patientProfileService;

    private static final Long ACCOUNT_ID = 1L;

    private PatientProfile profile;

    @BeforeEach
    void setUp() {
        Account account = new Account();
        account.setId(ACCOUNT_ID);
        account.setEmail("patient001@cbs.local");

        profile = new PatientProfile();
        profile.setId(100L);
        profile.setAccount(account);
        profile.setFullName("Nguyễn Văn An");
        profile.setDateOfBirth(LocalDate.of(1990, 1, 1));
        profile.setGender(Gender.MALE);
        profile.setPhoneNumber("0901234567");
    }

    private Authentication authOf(Long accountId) {
        return new UsernamePasswordAuthenticationToken(
                accountId, null, List.of(new SimpleGrantedAuthority("ROLE_PATIENT")));
    }

    // ---------- getMyProfile() ----------

    @Test
    void getMyProfile_shouldReturnProfile_whenExists() {
        when(patientProfileRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.of(profile));
        when(patientProfileMapper.toResponse(profile)).thenReturn(
                new PatientProfileResponse(100L, "patient001@cbs.local", "Nguyễn Văn An",
                        LocalDate.of(1990, 1, 1), Gender.MALE, "0901234567", null));

        PatientProfileResponse response = patientProfileService.getMyProfile(authOf(ACCOUNT_ID));

        assertThat(response.fullName()).isEqualTo("Nguyễn Văn An");
        assertThat(response.email()).isEqualTo("patient001@cbs.local");
    }

    @Test
    void getMyProfile_shouldReturn404_whenProfileNotFound() {
        when(patientProfileRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patientProfileService.getMyProfile(authOf(ACCOUNT_ID)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    // ---------- updateMyProfile() ----------

    @Test
    void updateMyProfile_shouldUpdateAndReturnProfile() {
        PatientProfileUpdateRequest request = new PatientProfileUpdateRequest(
                "Nguyễn Văn Bình", LocalDate.of(1991, 2, 2), Gender.OTHER, "0909999999", "123 Đường ABC");
        when(patientProfileRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.of(profile));
        when(patientProfileRepository.save(any(PatientProfile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(patientProfileMapper.toResponse(any(PatientProfile.class))).thenReturn(
                new PatientProfileResponse(100L, "patient001@cbs.local", "Nguyễn Văn Bình",
                        LocalDate.of(1991, 2, 2), Gender.OTHER, "0909999999", "123 Đường ABC"));

        PatientProfileResponse response = patientProfileService.updateMyProfile(request, authOf(ACCOUNT_ID));

        assertThat(response.fullName()).isEqualTo("Nguyễn Văn Bình");
        verify(patientProfileMapper).updateEntityFromRequest(request, profile);
        verify(patientProfileRepository).save(profile);
    }

    @Test
    void updateMyProfile_shouldReturn404_whenProfileNotFound() {
        PatientProfileUpdateRequest request = new PatientProfileUpdateRequest(
                "Nguyễn Văn Bình", LocalDate.of(1991, 2, 2), Gender.OTHER, "0909999999", "123 Đường ABC");
        when(patientProfileRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patientProfileService.updateMyProfile(request, authOf(ACCOUNT_ID)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));

        verify(patientProfileRepository, never()).save(any());
    }
}
