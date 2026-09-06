package com.clinicbookingbackend.controller.doctor;

import com.clinicbookingbackend.config.SecurityConfig;
import com.clinicbookingbackend.security.JwtPayload;
import com.clinicbookingbackend.security.JwtUtil;
import com.clinicbookingbackend.service.doctor.DoctorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Test riêng (không addFilters=false) để verify thật rule
// "/api/admin/**" -> hasRole("ADMIN") trong SecurityConfig — DoctorControllerTest
// tắt hẳn security filter nên không phủ được rule này (chỉ verify thủ công qua Swagger).
@WebMvcTest(DoctorController.class)
@Import(SecurityConfig.class)
class DoctorControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DoctorService doctorService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    void getAll_shouldBeRejected_whenNoToken() throws Exception {
        mockMvc.perform(get("/api/admin/doctors"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getAll_shouldReturn403_whenTokenRoleIsNotAdmin() throws Exception {
        when(jwtUtil.parseToken(anyString())).thenReturn(new JwtPayload(1L, "DOCTOR"));

        mockMvc.perform(get("/api/admin/doctors").header("Authorization", "Bearer fake-token"))
                .andExpect(status().isForbidden());
    }
}
