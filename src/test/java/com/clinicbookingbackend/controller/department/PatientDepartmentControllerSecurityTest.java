package com.clinicbookingbackend.controller.department;

import com.clinicbookingbackend.config.SecurityConfig;
import com.clinicbookingbackend.dto.doctor.DoctorSummaryResponse;
import com.clinicbookingbackend.security.JwtPayload;
import com.clinicbookingbackend.security.JwtUtil;
import com.clinicbookingbackend.service.doctor.DoctorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Verify rule "/api/departments/*/doctors" -> authenticated() (không giới hạn role) trong
// SecurityConfig (mirror DoctorScheduleControllerSecurityTest.java).
@WebMvcTest(PatientDepartmentController.class)
@Import(SecurityConfig.class)
class PatientDepartmentControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DoctorService doctorService;

    @MockitoBean
    private JwtUtil jwtUtil;

    private void mockDoctorServiceResponse() {
        DoctorSummaryResponse response = new DoctorSummaryResponse(100L, "Nguyễn Văn An", "Nội tổng quát", 1L);
        when(doctorService.getActiveDoctorsByDepartment(any(), any()))
                .thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1));
    }

    @Test
    void getActiveDoctorsByDepartment_shouldBeRejected_whenNoToken() throws Exception {
        mockMvc.perform(get("/api/departments/1/doctors"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getActiveDoctorsByDepartment_shouldBeAllowed_whenTokenRoleIsPatient() throws Exception {
        when(jwtUtil.parseToken(anyString())).thenReturn(new JwtPayload(1L, "PATIENT"));
        mockDoctorServiceResponse();

        mockMvc.perform(get("/api/departments/1/doctors")
                        .header("Authorization", "Bearer fake-token"))
                .andExpect(status().isOk());
    }

    @Test
    void getActiveDoctorsByDepartment_shouldBeAllowed_whenTokenRoleIsDoctor() throws Exception {
        when(jwtUtil.parseToken(anyString())).thenReturn(new JwtPayload(10L, "DOCTOR"));
        mockDoctorServiceResponse();

        mockMvc.perform(get("/api/departments/1/doctors")
                        .header("Authorization", "Bearer fake-token"))
                .andExpect(status().isOk());
    }

    @Test
    void getActiveDoctorsByDepartment_shouldBeAllowed_whenTokenRoleIsAdmin() throws Exception {
        when(jwtUtil.parseToken(anyString())).thenReturn(new JwtPayload(999L, "ADMIN"));
        mockDoctorServiceResponse();

        mockMvc.perform(get("/api/departments/1/doctors")
                        .header("Authorization", "Bearer fake-token"))
                .andExpect(status().isOk());
    }
}
