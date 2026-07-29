package com.brainserve.appointment.reporting.api;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
    private final JdbcTemplate jdbc;

    public DashboardController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/ceo")
    @PreAuthorize("hasRole('CEO')")
    DashboardView ceo() {
        return snapshot("CEO");
    }

    @GetMapping("/hr")
    @PreAuthorize("hasAnyRole('HR_ADMIN','HR_EXECUTIVE','CEO')")
    DashboardView hr() {
        return snapshot("HR");
    }

    @GetMapping("/employee")
    @PreAuthorize("hasRole('EMPLOYEE')")
    DashboardView employee() {
        return snapshot("EMPLOYEE");
    }

    @GetMapping("/reception")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','SECURITY','CEO')")
    DashboardView reception() {
        return snapshot("RECEPTION");
    }

    private DashboardView snapshot(String audience) {
        Instant now = Instant.now();
        Instant tomorrow = now.plus(1, ChronoUnit.DAYS);
        long expected = count(
                "select count(*) from appointments where starts_at >= ? and starts_at < ? and status = 'APPROVED'",
                now, tomorrow);
        long inside = count("select count(*) from visit_access_records where checked_in_at is not null and checked_out_at is null");
        long pending = count("select count(*) from appointments where status = 'PENDING_APPROVAL'");
        long employees = count("select count(*) from employees where status in ('ACTIVE','ONBOARDING')");
        long outbox = count("select count(*) from outbox_events where status = 'PENDING'");
        return new DashboardView(audience, now, Map.of(
                "expectedToday", expected,
                "visitorsInside", inside,
                "pendingApprovals", pending,
                "activeEmployees", employees,
                "notificationBacklog", outbox));
    }

    private long count(String sql, Object... arguments) {
        Long value = jdbc.queryForObject(sql, Long.class, arguments);
        return value == null ? 0 : value;
    }

    public record DashboardView(String audience, Instant generatedAt, Map<String, Long> metrics) {
    }
}
