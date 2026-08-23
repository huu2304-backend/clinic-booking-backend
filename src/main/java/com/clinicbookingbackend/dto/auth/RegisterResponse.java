package com.clinicbookingbackend.dto.auth;

public record RegisterResponse(
        Long id,
        String email,
        String fullName,
        String role) {
}