package com.clinicbookingbackend.controller.booking;

import com.clinicbookingbackend.config.SecurityConfig;
import com.clinicbookingbackend.dto.booking.DoctorAppointmentResponse;
import com.clinicbookingbackend.security.JwtPayload;
import com.clinicbookingbackend.security.JwtUtil;
import com.clinicbookingbackend.service.booking.BookingService;
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

// Test riêng (không addFilters=false) để verify thật rule
// "/api/doctors/me/appointments" -> hasRole("DOCTOR") trong SecurityConfig
// (mirror AppointmentControllerSecurityTest.java).
@WebMvcTest(DoctorAppointmentController.class)
@Import(SecurityConfig.class)
class DoctorAppointmentControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private JwtUtil jwtUtil;

    private void mockBookingServiceResponse() {
        when(bookingService.getMyAppointments(any(), any())).thenReturn(
                List.of(new DoctorAppointmentResponse(1L, LocalDate.of(2026, 9, 10),
                        LocalTime.of(9, 0), LocalTime.of(9, 30), "Nguyễn Văn An", "CONFIRMED")));
    }

    @Test
    void getMyAppointments_shouldBeRejected_whenNoToken() throws Exception {
        mockMvc.perform(get("/api/doctors/me/appointments").param("date", "2026-09-10"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getMyAppointments_shouldReturn403_whenTokenRoleIsPatient() throws Exception {
        when(jwtUtil.parseToken(anyString())).thenReturn(new JwtPayload(1L, "PATIENT"));

        mockMvc.perform(get("/api/doctors/me/appointments")
                        .header("Authorization", "Bearer fake-token")
                        .param("date", "2026-09-10"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMyAppointments_shouldReturn403_whenTokenRoleIsAdmin() throws Exception {
        when(jwtUtil.parseToken(anyString())).thenReturn(new JwtPayload(999L, "ADMIN"));

        mockMvc.perform(get("/api/doctors/me/appointments")
                        .header("Authorization", "Bearer fake-token")
                        .param("date", "2026-09-10"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMyAppointments_shouldBeAllowedByPathRule_whenTokenRoleIsDoctor() throws Exception {
        when(jwtUtil.parseToken(anyString())).thenReturn(new JwtPayload(10L, "DOCTOR"));
        mockBookingServiceResponse();

        mockMvc.perform(get("/api/doctors/me/appointments")
                        .header("Authorization", "Bearer fake-token")
                        .param("date", "2026-09-10"))
                .andExpect(status().isOk());
    }

    @Test
    void getMyAppointments_shouldReturn400_whenDateParamMissing() throws Exception {
        when(jwtUtil.parseToken(anyString())).thenReturn(new JwtPayload(10L, "DOCTOR"));

        mockMvc.perform(get("/api/doctors/me/appointments")
                        .header("Authorization", "Bearer fake-token"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMyAppointments_shouldReturn400_whenDateParamMalformed() throws Exception {
        when(jwtUtil.parseToken(anyString())).thenReturn(new JwtPayload(10L, "DOCTOR"));

        mockMvc.perform(get("/api/doctors/me/appointments")
                        .header("Authorization", "Bearer fake-token")
                        .param("date", "not-a-date"))
                .andExpect(status().isBadRequest());
    }
}
