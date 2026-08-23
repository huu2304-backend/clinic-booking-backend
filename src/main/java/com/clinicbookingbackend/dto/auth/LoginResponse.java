package com.clinicbookingbackend.dto.auth;

public record LoginResponse(
        String token,
        Long id,
        String email,
        String fullName,
        String role
) {
}