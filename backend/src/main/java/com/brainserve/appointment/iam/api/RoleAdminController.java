package com.brainserve.appointment.iam.api;

import com.brainserve.appointment.iam.application.RoleAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAuthority('ROLE_MANAGE')")
public class RoleAdminController {
    private final RoleAdminService service;

    public RoleAdminController(RoleAdminService service) {
        this.service = service;
    }

    @GetMapping("/roles")
    List<RoleAdminService.RoleView> roles() {
        return service.roles();
    }

    @GetMapping("/users")
    List<RoleAdminService.UserRoleView> users() {
        return service.users();
    }

    @PutMapping("/users/{id}/roles")
    RoleAdminService.UserRoleView replaceRoles(
            @PathVariable UUID id,
            @Valid @RequestBody RoleAssignmentRequest request) {
        return service.replaceUserRoles(id, request.roles());
    }

    record RoleAssignmentRequest(@NotEmpty Set<String> roles) {
    }
}
