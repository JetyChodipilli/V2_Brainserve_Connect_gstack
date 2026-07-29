package com.brainserve.appointment.availability.infrastructure;

import com.brainserve.appointment.availability.domain.AvailabilityRule;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvailabilityRuleRepository extends JpaRepository<AvailabilityRule, UUID> {
    List<AvailabilityRule> findAllByEmployeeIdAndActiveTrueOrderByDayOfWeekAscStartsAtAsc(UUID employeeId);
    void deleteAllByEmployeeId(UUID employeeId);
}
