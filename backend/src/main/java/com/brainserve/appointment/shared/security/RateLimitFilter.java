package com.brainserve.appointment.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final StringRedisTemplate redis;

    public RateLimitFilter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equals(request.getMethod())) return true;
        String path = request.getRequestURI();
        return !(path.equals("/api/v1/auth/login")
                || path.equals("/api/v1/public/appointments")
                || path.endsWith("/verify-otp"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String category = request.getRequestURI().contains("auth/login") ? "login" : "public";
        int limit = category.equals("login") ? 10 : 30;
        String key = "rate:" + category + ":" + request.getRemoteAddr();
        try {
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1) redis.expire(key, Duration.ofMinutes(1));
            if (count != null && count > limit) {
                response.setStatus(429);
                response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                response.setHeader("Retry-After", "60");
                response.getWriter().write("""
                        {"type":"https://brainserve.example/problems/rate-limit",
                         "title":"Too Many Requests","status":429,
                         "detail":"Please wait before trying again.",
                         "errorCode":"RATE_LIMITED","fieldErrors":[]}
                        """);
                return;
            }
        } catch (RuntimeException redisUnavailable) {
            response.setStatus(503);
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.getWriter().write("""
                    {"type":"https://brainserve.example/problems/rate-limit-unavailable",
                     "title":"Service Unavailable","status":503,
                     "detail":"Verification protection is temporarily unavailable.",
                     "errorCode":"RATE_LIMIT_UNAVAILABLE","fieldErrors":[]}
                    """);
            return;
        }
        chain.doFilter(request, response);
    }
}
