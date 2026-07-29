package com.brainserve.appointment.iam.application;

import com.brainserve.appointment.iam.domain.Role;
import com.brainserve.appointment.iam.domain.UserAccount;
import com.brainserve.appointment.iam.infrastructure.RoleRepository;
import com.brainserve.appointment.iam.infrastructure.UserAccountRepository;
import com.brainserve.appointment.shared.api.DomainException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentityDirectory {
    private final UserAccountRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;

    public IdentityDirectory(
            UserAccountRepository users,
            RoleRepository roles,
            PasswordEncoder passwordEncoder) {
        this.users = users;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public boolean loginExists(String login) {
        return users.existsByLoginIgnoreCase(login);
    }

    @Transactional
    public void provisionEmployee(UUID employeeId, String login, String displayName, String temporaryPassword) {
        provisionEmployeeWithRole(employeeId, login, displayName, temporaryPassword, "ROLE_EMPLOYEE");
    }

    @Transactional
    public void provisionPrivilegedEmployee(
            UUID employeeId,
            String login,
            String displayName,
            String temporaryPassword,
            String roleCode) {
        provisionEmployeeWithRole(employeeId, login, displayName, temporaryPassword, roleCode);
    }

    private void provisionEmployeeWithRole(
            UUID employeeId,
            String login,
            String displayName,
            String temporaryPassword,
            String roleCode) {
        Role employeeRole = roles.findByCode(roleCode)
                .orElseThrow(() -> new IllegalStateException("Required role is missing: " + roleCode));
        users.save(UserAccount.forEmployee(
                employeeId, login, displayName, passwordEncoder.encode(temporaryPassword), employeeRole));
    }

    @Transactional(readOnly = true)
    public UUID employeeIdForUser(UUID userId) {
        return users.findById(userId)
                .map(UserAccount::getEmployeeId)
                .orElseThrow(() -> new DomainException(
                        "EMPLOYEE_PROFILE_MISSING", HttpStatus.CONFLICT, "Employee profile is not linked."));
    }

    @Transactional(readOnly = true)
    public UUID optionalEmployeeIdForUser(UUID userId) {
        return users.findById(userId).map(UserAccount::getEmployeeId).orElse(null);
    }
}
