package com.brainserve.appointment.reception.infrastructure;

import com.brainserve.appointment.reception.domain.VisitorBadge;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface BadgeRepository extends JpaRepository<VisitorBadge, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<VisitorBadge> findFirstByActiveTrueAndAllocatedFalseOrderByBadgeNumber();
}
