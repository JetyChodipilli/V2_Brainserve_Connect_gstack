package com.brainserve.appointment.iam.api;

import com.brainserve.appointment.iam.application.AuthService;
import com.brainserve.appointment.shared.config.BrainServeProperties;
import com.brainserve.appointment.shared.security.CurrentUser;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private static final String REFRESH_COOKIE = "bs_refresh";
    private final AuthService service;
    private final CurrentUser currentUser;
    private final BrainServeProperties properties;

    public AuthController(AuthService service, CurrentUser currentUser, BrainServeProperties properties) {
        this.service = service;
        this.currentUser = currentUser;
        this.properties = properties;
    }

    @PostMapping("/login")
    ResponseEntity<SessionResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        AuthService.Session session = service.login(
                request.login(),
                request.password(),
                servletRequest.getHeader("User-Agent"),
                servletRequest.getRemoteAddr());
        return withCookie(session);
    }

    @PostMapping("/refresh")
    ResponseEntity<SessionResponse> refresh(
            @RequestHeader("X-Refresh-Intent") String refreshIntent,
            HttpServletRequest request) {
        if (!"rotate".equals(refreshIntent)) {
            return ResponseEntity.status(403).build();
        }
        AuthService.Session session = service.refresh(
                cookie(request),
                request.getHeader("User-Agent"),
                request.getRemoteAddr());
        return withCookie(session);
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(
            @RequestHeader("X-Refresh-Intent") String refreshIntent,
            HttpServletRequest request) {
        if ("revoke".equals(refreshIntent)) {
            service.logout(cookie(request));
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expiredCookie().toString())
                .build();
    }

    @PostMapping("/logout-all")
    ResponseEntity<Void> logoutAll() {
        service.logoutAll(currentUser.id());
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expiredCookie().toString())
                .build();
    }

    @PostMapping("/change-password")
    ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        service.changePassword(currentUser.id(), request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expiredCookie().toString())
                .build();
    }

    @GetMapping("/me")
    AuthService.UserView me() {
        return service.me(currentUser.id());
    }

    private ResponseEntity<SessionResponse> withCookie(AuthService.Session session) {
        SessionResponse body = new SessionResponse(session.accessToken(), session.expiresInSeconds(), session.user());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(session.refreshToken()).toString())
                .body(body);
    }

    private String cookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (REFRESH_COOKIE.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

    private ResponseCookie refreshCookie(String value) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(properties.secureCookies())
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(Duration.ofDays(14))
                .build();
    }

    private ResponseCookie expiredCookie() {
        return ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true).secure(properties.secureCookies()).sameSite("Strict")
                .path("/api/v1/auth").maxAge(Duration.ZERO).build();
    }

    public record LoginRequest(
            @NotBlank @Email String login,
            @NotBlank @Size(min = 12, max = 200) String password) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 12, max = 200) String newPassword) {
    }

    public record SessionResponse(
            String accessToken,
            long expiresInSeconds,
            AuthService.UserView user) {
    }
}
