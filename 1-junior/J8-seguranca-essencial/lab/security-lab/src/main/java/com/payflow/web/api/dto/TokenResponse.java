package com.payflow.web.api.dto;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) { }
