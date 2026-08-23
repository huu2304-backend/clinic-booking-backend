package com.clinicbookingbackend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                JwtPayload payload = jwtUtil.parseToken(token);

                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + payload.role()));

                var authentication = new UsernamePasswordAuthenticationToken(
                        payload.accountId(), null, authorities
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (Exception e) {
                // Token sai/hết hạn -> không set Authentication, coi như request ẩn danh
                // Spring Security tự chặn ở bước authorizeHttpRequests nếu endpoint yêu cầu đăng nhập
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}