package com.brainserve.appointment.iam.application;

import com.brainserve.appointment.iam.domain.Permission;
import com.brainserve.appointment.iam.domain.Role;
import com.brainserve.appointment.iam.domain.UserAccount;
import com.brainserve.appointment.shared.config.BrainServeProperties;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {
    private final JwtEncoder encoder;
    private final BrainServeProperties properties;

    public JwtTokenService(JwtEncoder encoder, BrainServeProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    public AccessToken issue(UserAccount user) {
        Instant now = Instant.now();
        Set<String> roles = user.getRoles().stream().map(Role::getCode).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> authorities = new LinkedHashSet<>(roles);
        authorities.addAll(permissions);
        JwtClaimsSet.Builder builder = JwtClaimsSet.builder()
                .issuer("brainserve-connect")
                .issuedAt(now)
                .expiresAt(now.plus(properties.accessTokenTtl()))
                .subject(user.getLogin())
                .claim("uid", user.getId().toString())
                .claim("name", user.getDisplayName())
                .claim("roles", roles)
                .claim("authorities", authorities)
                .claim("mustChangePassword", user.isMustChangePassword());
        if (user.getEmployeeId() != null) {
            builder.claim("employeeId", user.getEmployeeId().toString());
        }
        JwtClaimsSet claims = builder.build();
        String value = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new AccessToken(value, properties.accessTokenTtl().toSeconds(), roles, permissions);
    }

    public record AccessToken(
            String value,
            long expiresInSeconds,
            Set<String> roles,
            Set<String> permissions) {
    }
}
