package com.clinicbookingbackend.controller.doctorschedule;

import com.clinicbookingbackend.config.SecurityConfig;
import com.clinicbookingbackend.dto.doctorschedule.AvailableSlotResponse;
import com.clinicbookingbackend.security.JwtPayload;
import com.clinicbookingbackend.security.JwtUtil;
import com.clinicbookingbackend.service.doctorschedule.DoctorScheduleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Verify rule "/api/doctors/*/schedules" -> authenticated() (không giới hạn role) trong
// SecurityConfig, và validate query param "date" bắt buộc + đúng định dạng
// (mirror DoctorScheduleControllerSecurityTest.java).
@WebMvcTest(PatientDoctorScheduleController.class)
@Import(SecurityConfig.class)
class PatientDoctorScheduleControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DoctorScheduleService doctorScheduleService;

    @MockitoBean
    private JwtUtil jwtUtil;

    private void mockScheduleServiceResponse() {
        when(doctorScheduleService.getAvailableSlots(any(), any())).thenReturn(
                List.of(new AvailableSlotResponse(1L, LocalDate.of(2026, 9, 10), LocalTime.of(9, 0), LocalTime.of(9, 30))));
    }

    @Test
    void getAvailableSlots_shouldBeRejected_whenNoToken() throws Exception {
        mockMvc.perform(get("/api/doctors/1/schedules").param("date", "2026-09-10"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getAvailableSlots_shouldBeAllowed_whenTokenRoleIsPatient() throws Exception {
        when(jwtUtil.parseToken(anyString())).thenReturn(new JwtPayload(1L, "PATIENT"));
        mockScheduleServiceResponse();

        mockMvc.perform(get("/api/doctors/1/schedules")
                        .header("Authorization", "Bearer fake-token")
                        .param("date", "2026-09-10"))
                .andExpect(status().isOk());
    }

    @Test
    void getAvailableSlots_shouldBeAllowed_whenTokenRoleIsDoctor() throws Exception {
        when(jwtUtil.parseToken(anyString())).thenReturn(new JwtPayload(10L, "DOCTOR"));
        mockScheduleServiceResponse();

        mockMvc.perform(get("/api/doctors/1/schedules")
                        .header("Authorization", "Bearer fake-token")
                        .param("date", "2026-09-10"))
                .andExpect(status().isOk());
    }

    @Test
    void getAvailableSlots_shouldBeAllowed_whenTokenRoleIsAdmin() throws Exception {
        when(jwtUtil.parseToken(anyString())).thenReturn(new JwtPayload(999L, "ADMIN"));
        mockScheduleServiceResponse();

        mockMvc.perform(get("/api/doctors/1/schedules")
                        .header("Authorization", "Bearer fake-token")
                        .param("date", "2026-09-10"))
                .andExpect(status().isOk());
    }

    @Test
    void getAvailableSlots_shouldReturn400_whenDateParamMissing() throws Exception {
        when(jwtUtil.parseToken(anyString())).thenReturn(new JwtPayload(1L, "PATIENT"));

        mockMvc.perform(get("/api/doctors/1/schedules")
                        .header("Authorization", "Bearer fake-token"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAvailableSlots_shouldReturn400_whenDateParamMalformed() throws Exception {
        when(jwtUtil.parseToken(anyString())).thenReturn(new JwtPayload(1L, "PATIENT"));

        mockMvc.perform(get("/api/doctors/1/schedules")
                        .header("Authorization", "Bearer fake-token")
                        .param("date", "not-a-date"))
                .andExpect(status().isBadRequest());
    }
}
