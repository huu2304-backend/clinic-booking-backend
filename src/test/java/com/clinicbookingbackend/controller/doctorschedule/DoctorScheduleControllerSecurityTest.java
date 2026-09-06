package com.clinicbookingbackend.controller.doctorschedule;

import com.clinicbookingbackend.config.SecurityConfig;
import com.clinicbookingbackend.dto.doctorschedule.DoctorScheduleCreateRequest;
import com.clinicbookingbackend.dto.doctorschedule.DoctorScheduleResponse;
import com.clinicbookingbackend.entity.doctorschedule.enums.ScheduleStatus;
import com.clinicbookingbackend.security.JwtPayload;
import com.clinicbookingbackend.security.JwtUtil;
import com.clinicbookingbackend.service.doctorschedule.DoctorScheduleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Test riêng (không addFilters=false) để verify thật rule
// "/api/doctor-schedules/**" -> hasAnyRole("ADMIN","DOCTOR") trong SecurityConfig
// (mirror DoctorControllerSecurityTest.java). Check ownership (Doctor chỉ đụng slot của
// mình) nằm trong Service, không phải path-matcher, nên KHÔNG test được ở đây.
@WebMvcTest(DoctorScheduleController.class)
@Import(SecurityConfig.class)
class DoctorScheduleControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DoctorScheduleService doctorScheduleService;

    @MockitoBean
    private JwtUtil jwtUtil;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private String createRequestJson() throws Exception {
        return objectMapper.writeValueAsString(
                new DoctorScheduleCreateRequest(1L, LocalDate.of(2026, 9, 10), LocalTime.of(9, 0), LocalTime.of(9, 30)));
    }

    @Test
    void create_shouldBeRejected_whenNoToken() throws Exception {
        mockMvc.perform(post("/api/doctor-schedules")
                        .contentType("application/json")
                        .content(createRequestJson()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void create_shouldReturn403_whenTokenRoleIsPatient() throws Exception {
        when(jwtUtil.parseToken(anyString())).thenReturn(new JwtPayload(1L, "PATIENT"));

        mockMvc.perform(post("/api/doctor-schedules")
                        .header("Authorization", "Bearer fake-token")
                        .contentType("application/json")
                        .content(createRequestJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_shouldBeAllowedByPathRule_whenTokenRoleIsDoctor() throws Exception {
        when(jwtUtil.parseToken(anyString())).thenReturn(new JwtPayload(10L, "DOCTOR"));
        when(doctorScheduleService.create(any(), any())).thenReturn(
                new DoctorScheduleResponse(1L, 1L, "Nguyễn Văn An", LocalDate.of(2026, 9, 10), LocalTime.of(9, 0), LocalTime.of(9, 30), ScheduleStatus.AVAILABLE));

        mockMvc.perform(post("/api/doctor-schedules")
                        .header("Authorization", "Bearer fake-token")
                        .contentType("application/json")
                        .content(createRequestJson()))
                .andExpect(status().isCreated());
    }

    @Test
    void create_shouldBeAllowedByPathRule_whenTokenRoleIsAdmin() throws Exception {
        when(jwtUtil.parseToken(anyString())).thenReturn(new JwtPayload(999L, "ADMIN"));
        when(doctorScheduleService.create(any(), any())).thenReturn(
                new DoctorScheduleResponse(1L, 1L, "Nguyễn Văn An", LocalDate.of(2026, 9, 10), LocalTime.of(9, 0), LocalTime.of(9, 30), ScheduleStatus.AVAILABLE));

        mockMvc.perform(post("/api/doctor-schedules")
                        .header("Authorization", "Bearer fake-token")
                        .contentType("application/json")
                        .content(createRequestJson()))
                .andExpect(status().isCreated());
    }
}
