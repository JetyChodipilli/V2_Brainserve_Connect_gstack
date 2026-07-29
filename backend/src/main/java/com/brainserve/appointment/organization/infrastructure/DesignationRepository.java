package com.brainserve.appointment.organization.infrastructure;

import com.brainserve.appointment.organization.domain.Designation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DesignationRepository extends JpaRepository<Designation, UUID> {
    List<Designation> findAllByActiveTrueOrderByName();
    Optional<Designation> findFirstByDepartmentIdOrderByLevelDesc(UUID departmentId);
}
