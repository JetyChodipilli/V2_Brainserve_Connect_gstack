package com.brainserve.appointment.iam.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("brainserve.bootstrap")
public record BootstrapProperties(
        String ceoEmail,
        String ceoPassword,
        String adminEmail,
        String adminPassword) {
}
