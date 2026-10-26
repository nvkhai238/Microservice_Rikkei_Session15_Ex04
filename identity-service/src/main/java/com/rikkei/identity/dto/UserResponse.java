package com.rikkei.identity.dto;

public record UserResponse(
    String id,
    String username,
    String role
) {}
