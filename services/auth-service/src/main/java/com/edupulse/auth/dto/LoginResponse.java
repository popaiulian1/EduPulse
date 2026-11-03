package com.edupulse.auth.dto;

public record LoginResponse(String accessToken, String tokenType, long expiresIn) {

}
