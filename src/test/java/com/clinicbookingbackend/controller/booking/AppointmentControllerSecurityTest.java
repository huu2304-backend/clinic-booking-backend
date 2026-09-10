package com.clinicbookingbackend.controller.booking;

import com.clinicbookingbackend.config.SecurityConfig;
import com.clinicbookingbackend.dto.booking.AppointmentResponse;
import com.clinicbookingbackend.dto.booking.ConfirmAppointmentRequest;
import com.clinicbookingbackend.security.JwtPayload;
import com.clinicbookingbackend.security.JwtUtil;
import com.clinicbookingbackend.service.booking.BookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.anyLong;

// Test riêng (không addFilters=false) để verify thật rule
// "/api/appointments" POST -> hasRole("PATIENT") trong SecurityConfig
// (mirror DoctorScheduleControllerSecurityTest.java).
@WebMvcTest(AppointmentController.class)
@Import(SecurityConfig.class)
class AppointmentControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private JwtUtil jwtUtil;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private String confirmRequestJson() throws Exception {
        return objectMapper.writeValueAsString(new ConfirmAppointmentRequest(1L));
    }

    @Test
    void confirm_shouldBeRejected_whenNoToken() throws Exception {
        mockMvc.perform(post("/api/appointments")
                        .contentType("application/json")
                        .content(confirmRequestJson()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void confirm_shouldReturn403_whenTokenRoleIsDoctor() throws Exception {
        when(jwtUtil.parseToken(anyString())).thenReturn(new JwtPayload(10L, "DOCTOR"));

        mockMvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer fake-token")
                        .contentType("application/json")
                        .content(confirmRequestJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    void confirm_shouldReturn403_whenTokenRoleIsAdmin() throws Exception {
        when(jwtUtil.parseToken(anyString())).thenReturn(new JwtPayload(999L, "ADMIN"));

        mockMvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer fake-token")
                        .contentType("application/json")
                        .content(confirmRequestJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    void confirm_shouldBeAllowedByPathRule_whenTokenRoleIsPatient() throws Exception {
        when(jwtUtil.parseToken(anyString())).thenReturn(new JwtPayload(1L, "PATIENT"));
        when(bookingService.confirm(any(), any())).thenReturn(
                new AppointmentResponse(1L, 1L, 100L, "Nguyễn Văn An",
                        LocalDate.of(2026, 9, 10), LocalTime.of(9, 0), LocalTime.of(9, 30), "CONFIRMED", LocalDateTime.now()));

        mockMvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer fake-token")
                        .contentType("application/json")
                        .content(confirmRequestJson()))
                .andExpect(status().isCreated());
    }

    // ---------- GET /api/appointments (CBS-70) ----------

    @Test
    void getMyAppointments_shouldBeRejected_whenNoToken() throws Exception {
        mockMvc.perform(get("/api/appointments"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getMyAppointments_shouldReturn403_whenTokenRoleIsDoctor() throws Exception {
        when(jwtUtil.parseToken(anyString())).thenReturn(new JwtPayload(10L, "DOCTOR"));

        mockMvc.perform(get("/api/appointments")
                        .header("Authorization", "Bearer fake-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMyAppointments_shouldBeAllowedByPathRule_whenTokenRoleIsPatient() throws Exception {
        when(jwtUtil.parseToken(anyString())).thenReturn(new JwtPayload(1L, "PATIENT"));
        when(bookingService.getMyAppointments(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/appointments")
                        .header("Authorization", "Bearer fake-token"))
                .andExpect(status().isOk());
    }

    // ---------- POST /api/appointments/{id}/cancel ----------

    @Test
    void cancel_shouldBeRejected_whenNoToken() throws Exception {
        mockMvc.perform(post("/api/appointments/1/cancel"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void cancel_shouldReturn403_whenTokenRoleIsDoctor() throws Exception {
        when(jwtUtil.parseToken(anyString())).thenReturn(new JwtPayload(10L, "DOCTOR"));

        mockMvc.perform(post("/api/appointments/1/cancel")
                        .header("Authorization", "Bearer fake-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancel_shouldReturn403_whenTokenRoleIsAdmin() throws Exception {
        when(jwtUtil.parseToken(anyString())).thenReturn(new JwtPayload(999L, "ADMIN"));

        mockMvc.perform(post("/api/appointments/1/cancel")
                        .header("Authorization", "Bearer fake-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancel_shouldBeAllowedByPathRule_whenTokenRoleIsPatient() throws Exception {
        when(jwtUtil.parseToken(anyString())).thenReturn(new JwtPayload(1L, "PATIENT"));
        when(bookingService.cancel(anyLong(), any())).thenReturn(
                new AppointmentResponse(1L, 1L, 100L, "Nguyễn Văn An",
                        LocalDate.of(2026, 9, 10), LocalTime.of(9, 0), LocalTime.of(9, 30), "CANCELLED", LocalDateTime.now()));

        mockMvc.perform(post("/api/appointments/1/cancel")
                        .header("Authorization", "Bearer fake-token"))
                .andExpect(status().isOk());
    }
}
