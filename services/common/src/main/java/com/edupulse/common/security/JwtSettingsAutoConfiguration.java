package com.edupulse.common.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Registers {@link JwtSettings} in every service that has the OAuth2 JWT classes on its
 * classpath, servlet or reactive. Services without them (discovery-server) are unaffected.
 */
@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.security.oauth2.jwt.Jwt")
@EnableConfigurationProperties(JwtSettings.class)
public class JwtSettingsAutoConfiguration {
}
