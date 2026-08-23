package com.clinicbookingbackend.security;

public record JwtPayload(
        Long accountId,
        String role
) {
}