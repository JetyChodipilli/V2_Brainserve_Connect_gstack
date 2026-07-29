package com.brainserve.appointment.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PostgresMigrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16.4-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("brainserve.security.jwt-secret",
                () -> "test-only-secret-with-more-than-thirty-two-bytes");
    }

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void migrationCreatesPermissionsWithoutGrantingSalaryToSystemAdmin() {
        Integer permissions = jdbc.queryForObject("select count(*) from permissions", Integer.class);
        Integer salaryForSystemAdmin = jdbc.queryForObject("""
                select count(*)
                from role_permissions rp
                join roles r on r.id = rp.role_id
                join permissions p on p.id = rp.permission_id
                where r.code = 'ROLE_SYSTEM_ADMIN' and p.code like 'SALARY_%'
                """, Integer.class);
        assertThat(permissions).isGreaterThan(20);
        assertThat(salaryForSystemAdmin).isZero();
    }
}
