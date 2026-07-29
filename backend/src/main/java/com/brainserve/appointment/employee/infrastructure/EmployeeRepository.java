package com.brainserve.appointment.employee.infrastructure;

import com.brainserve.appointment.employee.domain.Employee;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
    boolean existsByOfficialEmailIgnoreCase(String email);
    Page<Employee> findAllByOrderByDisplayName(Pageable pageable);
    List<Employee> findAllByStatusOrderByDisplayName(com.brainserve.appointment.employee.domain.EmployeeStatus status);

    @Query(value = "select nextval('employee_business_id_seq')", nativeQuery = true)
    long nextBusinessSequence();
}
