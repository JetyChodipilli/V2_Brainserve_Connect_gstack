package com.brainserve.appointment.compensation.infrastructure;

import com.brainserve.appointment.compensation.domain.CompensationPackage;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CompensationRepository extends JpaRepository<CompensationPackage, UUID> {
    List<CompensationPackage> findAllByEmployeeIdOrderByEffectiveFromDesc(UUID employeeId);

    @Query("""
            select c from CompensationPackage c
            where c.employeeId = :employeeId
              and c.status = 'APPROVED'
              and c.effectiveFrom <= :today
              and (c.effectiveTo is null or c.effectiveTo >= :today)
            order by c.effectiveFrom desc
            """)
    List<CompensationPackage> current(UUID employeeId, LocalDate today);

    @Query("""
            select count(c) > 0 from CompensationPackage c
            where c.employeeId = :employeeId
              and c.status in ('PROPOSED','APPROVED')
              and c.effectiveFrom <= coalesce(:effectiveTo, c.effectiveFrom)
              and (c.effectiveTo is null or c.effectiveTo >= :effectiveFrom)
            """)
    boolean hasOverlap(UUID employeeId, LocalDate effectiveFrom, LocalDate effectiveTo);

    List<CompensationPackage> findAllByStatusOrderByCreatedAt(String status);
}
