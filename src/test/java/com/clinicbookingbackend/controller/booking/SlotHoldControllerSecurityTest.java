package com.clinicbookingbackend.controller.booking;

import com.clinicbookingbackend.config.SecurityConfig;
import com.clinicbookingbackend.dto.booking.HoldResponse;
import com.clinicbookingbackend.security.JwtPayload;
import com.clinicbookingbackend.security.JwtUtil;
import com.clinicbookingbackend.service.booking.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Test riêng (không addFilters=false) để verify thật rule
// "/api/schedules/*/hold|release-hold" -> hasRole("PATIENT") trong SecurityConfig
// (mirror DoctorScheduleControllerSecurityTest.java).
@WebMvcTest(SlotHoldController.class)
@Import(SecurityConfig.class)
class SlotHoldControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    void hold_shouldBeRejected_whenNoToken() throws Exception {
        mockMvc.perform(post("/api/schedules/1/hold"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void hold_shouldReturn403_whenTokenRoleIsDoctor() throws Exception {
        when(jwtUtil.parseToken(anyString())).thenReturn(new JwtPayload(10L, "DOCTOR"));

        mockMvc.perform(post("/api/schedules/1/hold")
                        .header("Authorization", "Bearer fake-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void hold_shouldReturn403_whenTokenRoleIsAdmin() throws Exception {
        when(jwtUtil.parseToken(anyString())).thenReturn(new JwtPayload(999L, "ADMIN"));

        mockMvc.perform(post("/api/schedules/1/hold")
                        .header("Authorization", "Bearer fake-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void hold_shouldBeAllowedByPathRule_whenTokenRoleIsPatient() throws Exception {
        when(jwtUtil.parseToken(anyString())).thenReturn(new JwtPayload(1L, "PATIENT"));
        when(bookingService.hold(eq(1L), any())).thenReturn(new HoldResponse(1L, LocalDateTime.now().plusMinutes(5)));

        mockMvc.perform(post("/api/schedules/1/hold")
                        .header("Authorization", "Bearer fake-token"))
                .andExpect(status().isOk());
    }

    @Test
    void releaseHold_shouldReturn403_whenTokenRoleIsDoctor() throws Exception {
        when(jwtUtil.parseToken(anyString())).thenReturn(new JwtPayload(10L, "DOCTOR"));

        mockMvc.perform(post("/api/schedules/1/release-hold")
                        .header("Authorization", "Bearer fake-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void releaseHold_shouldBeAllowedByPathRule_whenTokenRoleIsPatient() throws Exception {
        when(jwtUtil.parseToken(anyString())).thenReturn(new JwtPayload(1L, "PATIENT"));

        mockMvc.perform(post("/api/schedules/1/release-hold")
                        .header("Authorization", "Bearer fake-token"))
                .andExpect(status().isNoContent());
    }
}
