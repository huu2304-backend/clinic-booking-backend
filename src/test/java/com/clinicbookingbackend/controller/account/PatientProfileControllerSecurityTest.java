package com.clinicbookingbackend.controller.account;

import com.clinicbookingbackend.config.SecurityConfig;
import com.clinicbookingbackend.dto.account.PatientProfileResponse;
import com.clinicbookingbackend.dto.account.PatientProfileUpdateRequest;
import com.clinicbookingbackend.entity.account.enums.Gender;
import com.clinicbookingbackend.security.JwtPayload;
import com.clinicbookingbackend.security.JwtUtil;
import com.clinicbookingbackend.service.account.PatientProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Test riêng (không addFilters=false) để verify thật rule "/api/patients/me" -> hasRole("PATIENT")
// trong SecurityConfig (mirror AppointmentControllerSecurityTest.java). Vì Authentication chỉ
// được set khi JwtAuthenticationFilter thật sự chạy, các case validate 400 cũng đặt ở đây (cần
// JWT hợp lệ để request đi qua tới @Valid).
@WebMvcTest(PatientProfileController.class)
@Import(SecurityConfig.class)
class PatientProfileControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PatientProfileService patientProfileService;

    @MockitoBean
    private JwtUtil jwtUtil;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private String validUpdateRequestJson() throws Exception {
        return objectMapper.writeValueAsString(new PatientProfileUpdateRequest(
                "Nguyễn Văn An", LocalDate.of(1990, 1, 1), Gender.MALE, "0901234567", "123 Đường ABC"));
    }

    private void mockPatientToken() {
        when(jwtUtil.parseToken(anyString())).thenReturn(new JwtPayload(1L, "PATIENT"));
    }

    // ---------- GET /api/patients/me ----------

    @Test
    void getMyProfile_shouldBeRejected_whenNoToken() throws Exception {
        mockMvc.perform(get("/api/patients/me"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getMyProfile_shouldReturn403_whenTokenRoleIsDoctor() throws Exception {
        when(jwtUtil.parseToken(anyString())).thenReturn(new JwtPayload(10L, "DOCTOR"));

        mockMvc.perform(get("/api/patients/me").header("Authorization", "Bearer fake-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMyProfile_shouldReturn403_whenTokenRoleIsAdmin() throws Exception {
        when(jwtUtil.parseToken(anyString())).thenReturn(new JwtPayload(999L, "ADMIN"));

        mockMvc.perform(get("/api/patients/me").header("Authorization", "Bearer fake-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMyProfile_shouldBeAllowedByPathRule_whenTokenRoleIsPatient() throws Exception {
        mockPatientToken();
        when(patientProfileService.getMyProfile(any())).thenReturn(
                new PatientProfileResponse(1L, "patient001@cbs.local", "Nguyễn Văn An",
                        LocalDate.of(1990, 1, 1), Gender.MALE, "0901234567", "123 Đường ABC"));

        mockMvc.perform(get("/api/patients/me").header("Authorization", "Bearer fake-token"))
                .andExpect(status().isOk());
    }

    // ---------- PUT /api/patients/me ----------

    @Test
    void updateMyProfile_shouldBeRejected_whenNoToken() throws Exception {
        mockMvc.perform(put("/api/patients/me")
                        .contentType("application/json")
                        .content(validUpdateRequestJson()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void updateMyProfile_shouldReturn403_whenTokenRoleIsDoctor() throws Exception {
        when(jwtUtil.parseToken(anyString())).thenReturn(new JwtPayload(10L, "DOCTOR"));

        mockMvc.perform(put("/api/patients/me")
                        .header("Authorization", "Bearer fake-token")
                        .contentType("application/json")
                        .content(validUpdateRequestJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateMyProfile_shouldReturn403_whenTokenRoleIsAdmin() throws Exception {
        when(jwtUtil.parseToken(anyString())).thenReturn(new JwtPayload(999L, "ADMIN"));

        mockMvc.perform(put("/api/patients/me")
                        .header("Authorization", "Bearer fake-token")
                        .contentType("application/json")
                        .content(validUpdateRequestJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateMyProfile_shouldBeAllowedByPathRule_whenTokenRoleIsPatient() throws Exception {
        mockPatientToken();
        when(patientProfileService.updateMyProfile(any(), any())).thenReturn(
                new PatientProfileResponse(1L, "patient001@cbs.local", "Nguyễn Văn An",
                        LocalDate.of(1990, 1, 1), Gender.MALE, "0901234567", "123 Đường ABC"));

        mockMvc.perform(put("/api/patients/me")
                        .header("Authorization", "Bearer fake-token")
                        .contentType("application/json")
                        .content(validUpdateRequestJson()))
                .andExpect(status().isOk());
    }

    @Test
    void updateMyProfile_shouldReturn400_whenFullNameBlank() throws Exception {
        mockPatientToken();
        String json = objectMapper.writeValueAsString(new PatientProfileUpdateRequest(
                "  ", LocalDate.of(1990, 1, 1), Gender.MALE, "0901234567", "123 Đường ABC"));

        mockMvc.perform(put("/api/patients/me")
                        .header("Authorization", "Bearer fake-token")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateMyProfile_shouldReturn400_whenPhoneNumberInvalid() throws Exception {
        mockPatientToken();
        String json = objectMapper.writeValueAsString(new PatientProfileUpdateRequest(
                "Nguyễn Văn An", LocalDate.of(1990, 1, 1), Gender.MALE, "abc123", "123 Đường ABC"));

        mockMvc.perform(put("/api/patients/me")
                        .header("Authorization", "Bearer fake-token")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateMyProfile_shouldReturn400_whenDateOfBirthInFuture() throws Exception {
        mockPatientToken();
        String json = objectMapper.writeValueAsString(new PatientProfileUpdateRequest(
                "Nguyễn Văn An", LocalDate.now().plusDays(1), Gender.MALE, "0901234567", "123 Đường ABC"));

        mockMvc.perform(put("/api/patients/me")
                        .header("Authorization", "Bearer fake-token")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isBadRequest());
    }
}
