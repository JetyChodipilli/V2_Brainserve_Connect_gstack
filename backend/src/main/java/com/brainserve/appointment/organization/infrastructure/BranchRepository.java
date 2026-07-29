package com.brainserve.appointment.organization.infrastructure;

import com.brainserve.appointment.organization.domain.Branch;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRepository extends JpaRepository<Branch, UUID> {
    Optional<Branch> findByCodeIgnoreCase(String code);
}
