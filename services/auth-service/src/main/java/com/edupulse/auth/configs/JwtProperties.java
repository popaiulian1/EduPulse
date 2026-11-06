package com.edupulse.auth.configs;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("jwt")
public record JwtProperties(
    RSAPublicKey publicKey,
    RSAPrivateKey privateKey,
    @DefaultValue("1h") Duration expiresIn,
    String issuer) {
}
