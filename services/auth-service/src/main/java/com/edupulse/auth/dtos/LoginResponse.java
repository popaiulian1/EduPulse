package com.edupulse.auth.dtos;

public record LoginResponse(String accessToken, String tokenType, long expiresIn) {

}
