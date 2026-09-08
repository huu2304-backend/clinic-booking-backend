package com.clinicbookingbackend.config;

import com.clinicbookingbackend.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN") // Chỉ ADMIN mới được gọi các API quản trị
                        .requestMatchers("/api/doctor-schedules/**").hasAnyRole("ADMIN", "DOCTOR") // Ownership check (chỉ chủ sở hữu) nằm trong Service
                        .requestMatchers("/api/departments/*/doctors", "/api/doctors/*/schedules").authenticated() // Patient xem khoa/lịch trống (CBS-39) — mọi role đã login đều xem được
                        .requestMatchers("/api/doctors/me/appointments").hasRole("DOCTOR") // Doctor xem Appointment trong ngày của mình (CBS-53) — chỉ Doctor
                        .requestMatchers("/api/patients/me").hasRole("PATIENT") // Xem/sửa hồ sơ cá nhân (CBS-66) — chỉ Patient
                        .requestMatchers(HttpMethod.POST, "/api/schedules/*/hold", "/api/schedules/*/release-hold").hasRole("PATIENT") // Giữ chỗ/hủy giữ chỗ (CBS-72) — chỉ Patient
                        .requestMatchers(HttpMethod.POST, "/api/appointments").hasRole("PATIENT") // Xác nhận đặt lịch (CBS-42) — chỉ Patient
                        .requestMatchers(HttpMethod.POST, "/api/appointments/*/cancel").hasRole("PATIENT") // Hủy lịch hẹn (CBS-51) — chỉ Patient, ownership check trong Service
                        .anyRequest().authenticated() // MỌI API khác bắt buộc phải có JWT hợp lệ
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}