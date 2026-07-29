package com.brainserve.appointment.iam.infrastructure;

import com.brainserve.appointment.iam.domain.Role;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, UUID> {
    Optional<Role> findByCode(String code);
    List<Role> findAllByCodeIn(Collection<String> codes);
}
