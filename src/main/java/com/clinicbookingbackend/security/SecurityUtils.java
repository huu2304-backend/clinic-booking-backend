package com.clinicbookingbackend.security;

import org.springframework.security.core.Authentication;

// JWT filter đặt Long accountId thô làm principal (không phải UserDetails) — helper này
// tập trung 2 thao tác lặp lại ở mọi nơi cần check "resource ownership" (user chỉ được đụng
// resource của chính mình), việc mà path-matcher trong SecurityConfig không biểu đạt được.
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Long getCurrentAccountId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }

    public static boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }
}
