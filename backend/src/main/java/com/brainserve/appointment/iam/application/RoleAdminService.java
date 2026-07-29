package com.brainserve.appointment.iam.application;

import com.brainserve.appointment.iam.domain.Permission;
import com.brainserve.appointment.iam.domain.Role;
import com.brainserve.appointment.iam.domain.UserAccount;
import com.brainserve.appointment.iam.infrastructure.RoleRepository;
import com.brainserve.appointment.iam.infrastructure.UserAccountRepository;
import com.brainserve.appointment.shared.api.DomainException;
import com.brainserve.appointment.shared.audit.AuditService;
import com.brainserve.appointment.shared.security.CurrentUser;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleAdminService {
    private final RoleRepository roles;
    private final UserAccountRepository users;
    private final CurrentUser currentUser;
    private final AuditService audit;

    public RoleAdminService(
            RoleRepository roles,
            UserAccountRepository users,
            CurrentUser currentUser,
            AuditService audit) {
        this.roles = roles;
        this.users = users;
        this.currentUser = currentUser;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<RoleView> roles() {
        return roles.findAll().stream()
                .sorted(Comparator.comparing(Role::getCode))
                .map(RoleView::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserRoleView> users() {
        return users.findAll().stream()
                .sorted(Comparator.comparing(UserAccount::getLogin))
                .map(UserRoleView::from)
                .toList();
    }

    @Transactional
    public UserRoleView replaceUserRoles(UUID userId, Set<String> roleCodes) {
        if (currentUser.id().equals(userId)) {
            throw new DomainException(
                    "SELF_ROLE_CHANGE_FORBIDDEN",
                    HttpStatus.FORBIDDEN,
                    "Administrators cannot change their own privileged roles.");
        }
        if (roleCodes == null || roleCodes.isEmpty()) {
            throw new DomainException("ROLE_REQUIRED", HttpStatus.BAD_REQUEST, "Select at least one role.");
        }
        Set<String> normalized = roleCodes.stream()
                .map(String::trim)
                .map(code -> code.toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
        Set<Role> replacement = Set.copyOf(roles.findAllByCodeIn(normalized));
        if (replacement.size() != normalized.size()) {
            throw new DomainException("ROLE_INVALID", HttpStatus.BAD_REQUEST, "One or more roles are invalid.");
        }
        boolean systemAdmin = normalized.contains("ROLE_SYSTEM_ADMIN");
        boolean salaryGrant = replacement.stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getCode)
                .anyMatch(code -> code.startsWith("SALARY_"));
        if (systemAdmin && salaryGrant) {
            throw new DomainException(
                    "SYSTEM_ADMIN_SALARY_SEPARATION",
                    HttpStatus.CONFLICT,
                    "System administrators cannot hold a salary-granting role.");
        }
        UserAccount target = users.findById(userId)
                .orElseThrow(() -> new DomainException("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "User was not found."));
        target.replaceRoles(replacement);
        audit.record(currentUser.id(), "REPLACE_ROLES", "USER_ACCOUNT", userId,
                String.join(",", normalized), "RESTRICTED");
        return UserRoleView.from(target);
    }

    public record RoleView(
            UUID id,
            String code,
            String name,
            boolean systemRole,
            List<String> permissions) {
        static RoleView from(Role role) {
            return new RoleView(
                    role.getId(),
                    role.getCode(),
                    role.getName(),
                    role.isSystemRole(),
                    role.getPermissions().stream().map(Permission::getCode).sorted().toList());
        }
    }

    public record UserRoleView(
            UUID id,
            String login,
            String displayName,
            boolean enabled,
            List<String> roles) {
        static UserRoleView from(UserAccount user) {
            return new UserRoleView(
                    user.getId(),
                    user.getLogin(),
                    user.getDisplayName(),
                    user.isEnabled(),
                    user.getRoles().stream().map(Role::getCode).sorted().toList());
        }
    }
}
