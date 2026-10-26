package com.rikkei.identity.dto;

public record TokenResponseDTO(
    String accessToken,
    String refreshToken
) {}
