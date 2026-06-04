package com.example.ticketing.auth.dto;

public record LoginResponse(String accessToken, String userId, String name) {}
