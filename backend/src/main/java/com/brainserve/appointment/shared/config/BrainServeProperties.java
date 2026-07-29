package com.brainserve.appointment.shared.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("brainserve.security")
public record BrainServeProperties(
        String jwtSecret,
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        String allowedOrigins,
        int maxLoginFailures,
        Duration lockDuration,
        boolean secureCookies) {

    public BrainServeProperties {
        accessTokenTtl = accessTokenTtl == null ? Duration.ofMinutes(10) : accessTokenTtl;
        refreshTokenTtl = refreshTokenTtl == null ? Duration.ofDays(14) : refreshTokenTtl;
        allowedOrigins = allowedOrigins == null ? "" : allowedOrigins;
        maxLoginFailures = maxLoginFailures <= 0 ? 5 : maxLoginFailures;
        lockDuration = lockDuration == null ? Duration.ofMinutes(15) : lockDuration;
    }
}
