package com.brainserve.appointment.iam.application;

import com.brainserve.appointment.iam.domain.RefreshTokenSession;
import com.brainserve.appointment.iam.domain.UserAccount;
import com.brainserve.appointment.iam.infrastructure.RefreshTokenRepository;
import com.brainserve.appointment.iam.infrastructure.UserAccountRepository;
import com.brainserve.appointment.shared.api.DomainException;
import com.brainserve.appointment.shared.audit.AuditService;
import com.brainserve.appointment.shared.config.BrainServeProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserAccountRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokenService;
    private final BrainServeProperties properties;
    private final AuditService audit;
    private final SecureRandom random = new SecureRandom();

    public AuthService(
            UserAccountRepository users,
            RefreshTokenRepository refreshTokens,
            PasswordEncoder passwordEncoder,
            JwtTokenService tokenService,
            BrainServeProperties properties,
            AuditService audit) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.properties = properties;
        this.audit = audit;
    }

    @Transactional
    public Session login(String login, String password, String userAgent, String ipAddress) {
        UserAccount user = users.findByLoginIgnoreCase(login)
                .orElseThrow(this::invalidCredentials);
        if (!user.isEnabled() || user.isLocked()) {
            audit.record(user.getId(), "LOGIN_BLOCKED", "USER_ACCOUNT", user.getId(), null, "SECURITY");
            throw new DomainException("ACCOUNT_UNAVAILABLE", HttpStatus.LOCKED, "The account is disabled or temporarily locked.");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            user.failedLogin(properties.maxLoginFailures(), Instant.now().plus(properties.lockDuration()));
            audit.record(user.getId(), "LOGIN_FAILED", "USER_ACCOUNT", user.getId(), null, "SECURITY");
            throw invalidCredentials();
        }
        user.successfulLogin();
        JwtTokenService.AccessToken accessToken = tokenService.issue(user);
        String rawRefresh = randomToken();
        RefreshTokenSession refresh = new RefreshTokenSession(
                null,
                user.getId(),
                hash(rawRefresh),
                Instant.now().plus(properties.refreshTokenTtl()),
                userAgent,
                ipAddress);
        refreshTokens.save(refresh);
        audit.record(user.getId(), "LOGIN_SUCCEEDED", "USER_ACCOUNT", user.getId(), null, "SECURITY");
        return session(user, accessToken, rawRefresh);
    }

    @Transactional
    public Session refresh(String rawToken, String userAgent, String ipAddress) {
        RefreshTokenSession current = refreshTokens.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new DomainException("REFRESH_INVALID", HttpStatus.UNAUTHORIZED, "Refresh session is invalid."));
        if (!current.active()) {
            refreshTokens.revokeFamily(current.getFamilyId());
            audit.record(current.getUserId(), "REFRESH_REPLAY", "USER_ACCOUNT", current.getUserId(), null, "SECURITY");
            throw new DomainException("REFRESH_REUSED", HttpStatus.UNAUTHORIZED, "Refresh session was revoked.");
        }
        UserAccount user = users.findById(current.getUserId())
                .filter(UserAccount::isEnabled)
                .orElseThrow(() -> new DomainException("ACCOUNT_UNAVAILABLE", HttpStatus.UNAUTHORIZED, "The account is unavailable."));
        String replacementRaw = randomToken();
        RefreshTokenSession replacement = new RefreshTokenSession(
                current.getFamilyId(),
                user.getId(),
                hash(replacementRaw),
                Instant.now().plus(properties.refreshTokenTtl()),
                userAgent,
                ipAddress);
        refreshTokens.save(replacement);
        current.rotateTo(replacement.getId());
        return session(user, tokenService.issue(user), replacementRaw);
    }

    @Transactional
    public void logout(String rawToken) {
        refreshTokens.findByTokenHash(hash(rawToken)).ifPresent(RefreshTokenSession::revoke);
    }

    @Transactional
    public void logoutAll(UUID userId) {
        refreshTokens.findAllByUserIdAndRevokedAtIsNull(userId).forEach(RefreshTokenSession::revoke);
        audit.record(userId, "LOGOUT_ALL", "USER_ACCOUNT", userId, null, "SECURITY");
    }

    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        UserAccount user = users.findById(userId)
                .orElseThrow(() -> new DomainException("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "User account was not found."));
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw invalidCredentials();
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new DomainException("PASSWORD_REUSED", HttpStatus.BAD_REQUEST, "New password must differ from the current password.");
        }
        user.changePassword(passwordEncoder.encode(newPassword));
        logoutAll(userId);
        audit.record(userId, "PASSWORD_CHANGED", "USER_ACCOUNT", userId, null, "SECURITY");
    }

    @Transactional(readOnly = true)
    public UserView me(UUID userId) {
        UserAccount user = users.findById(userId)
                .orElseThrow(() -> new DomainException("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "User account was not found."));
        JwtTokenService.AccessToken grants = tokenService.issue(user);
        return new UserView(user.getId(), user.getEmployeeId(), user.getDisplayName(), user.getLogin(),
                grants.roles(), grants.permissions(), user.isMustChangePassword());
    }

    private Session session(UserAccount user, JwtTokenService.AccessToken token, String refreshToken) {
        return new Session(
                token.value(),
                token.expiresInSeconds(),
                refreshToken,
                new UserView(user.getId(), user.getEmployeeId(), user.getDisplayName(), user.getLogin(),
                        token.roles(), token.permissions(), user.isMustChangePassword()));
    }

    private String randomToken() {
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        if (token == null || token.isBlank()) {
            throw new DomainException("REFRESH_REQUIRED", HttpStatus.UNAUTHORIZED, "Refresh session is required.");
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private DomainException invalidCredentials() {
        return new DomainException("INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "Login or password is incorrect.");
    }

    public record Session(String accessToken, long expiresInSeconds, String refreshToken, UserView user) {
    }

    public record UserView(
            UUID id,
            UUID employeeId,
            String displayName,
            String login,
            java.util.Set<String> roles,
            java.util.Set<String> permissions,
            boolean mustChangePassword) {
    }
}
