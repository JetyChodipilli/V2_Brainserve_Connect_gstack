package com.brainserve.appointment.iam.application;

import com.brainserve.appointment.iam.domain.Role;
import com.brainserve.appointment.iam.domain.UserAccount;
import com.brainserve.appointment.iam.infrastructure.RoleRepository;
import com.brainserve.appointment.iam.infrastructure.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(1)
public class BootstrapService implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(BootstrapService.class);
    private final BootstrapProperties properties;
    private final UserAccountRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;

    public BootstrapService(
            BootstrapProperties properties,
            UserAccountRepository users,
            RoleRepository roles,
            PasswordEncoder encoder) {
        this.properties = properties;
        this.users = users;
        this.roles = roles;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seed(properties.adminEmail(), properties.adminPassword(), "BrainServe System Admin", "ROLE_SYSTEM_ADMIN");
    }

    private void seed(String email, String password, String displayName, String roleCode) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            log.info("Bootstrap account {} was not requested", roleCode);
            return;
        }
        if (password.length() < 12) {
            throw new IllegalStateException("Bootstrap passwords must contain at least 12 characters");
        }
        if (users.existsByLoginIgnoreCase(email)) {
            return;
        }
        Role role = roles.findByCode(roleCode)
                .orElseThrow(() -> new IllegalStateException("Required role is missing: " + roleCode));
        users.save(UserAccount.bootstrap(email, displayName, encoder.encode(password), role));
        log.info("Bootstrap account created for role {}; first-login password change is required", roleCode);
    }
}
