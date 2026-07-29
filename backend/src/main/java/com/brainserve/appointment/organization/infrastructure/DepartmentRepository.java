package com.brainserve.appointment.organization.infrastructure;

import com.brainserve.appointment.organization.domain.Department;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {
    List<Department> findAllByActiveTrueOrderByName();
    boolean existsByCodeIgnoreCase(String code);
    Optional<Department> findByCodeIgnoreCase(String code);
}
