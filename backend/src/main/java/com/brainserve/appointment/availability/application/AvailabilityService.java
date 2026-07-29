package com.brainserve.appointment.availability.application;

import com.brainserve.appointment.appointment.application.AppointmentService;
import com.brainserve.appointment.availability.domain.AvailabilityRule;
import com.brainserve.appointment.availability.infrastructure.AvailabilityRuleRepository;
import com.brainserve.appointment.employee.application.EmployeeService;
import com.brainserve.appointment.iam.application.IdentityDirectory;
import com.brainserve.appointment.shared.api.DomainException;
import com.brainserve.appointment.shared.security.CurrentUser;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvailabilityService {
    private static final ZoneId OFFICE_ZONE = ZoneId.of("Asia/Kolkata");
    private final AvailabilityRuleRepository rules;
    private final AppointmentService appointments;
    private final EmployeeService employees;
    private final IdentityDirectory identity;
    private final CurrentUser currentUser;

    public AvailabilityService(
            AvailabilityRuleRepository rules,
            AppointmentService appointments,
            EmployeeService employees,
            IdentityDirectory identity,
            CurrentUser currentUser) {
        this.rules = rules;
        this.appointments = appointments;
        this.employees = employees;
        this.identity = identity;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<Slot> slots(UUID employeeId, LocalDate date) {
        employees.publicHost(employeeId);
        if (date.isBefore(LocalDate.now(OFFICE_ZONE))) return List.of();
        List<AvailabilityRule> matching = rules.findAllByEmployeeIdAndActiveTrueOrderByDayOfWeekAscStartsAtAsc(employeeId)
                .stream().filter(rule -> rule.getDayOfWeek() == date.getDayOfWeek()).toList();
        if (matching.isEmpty() && date.getDayOfWeek().getValue() <= DayOfWeek.FRIDAY.getValue()) {
            matching = List.of(new AvailabilityRule(employeeId, date.getDayOfWeek(),
                    LocalTime.of(9, 30), LocalTime.of(17, 30), 30, 10));
        }
        List<Slot> result = new ArrayList<>();
        for (AvailabilityRule rule : matching) {
            ZonedDateTime cursor = ZonedDateTime.of(date, rule.getStartsAt(), OFFICE_ZONE);
            ZonedDateTime end = ZonedDateTime.of(date, rule.getEndsAt(), OFFICE_ZONE);
            while (!cursor.plusMinutes(rule.getSlotMinutes()).isAfter(end)) {
                Instant startsAt = cursor.toInstant();
                Instant endsAt = cursor.plusMinutes(rule.getSlotMinutes()).toInstant();
                if (startsAt.isAfter(Instant.now()) && !appointments.slotTaken(employeeId, startsAt)) {
                    result.add(new Slot(startsAt, endsAt));
                }
                cursor = cursor.plusMinutes(rule.getSlotMinutes() + rule.getBufferMinutes());
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<RuleView> mine() {
        return rules.findAllByEmployeeIdAndActiveTrueOrderByDayOfWeekAscStartsAtAsc(ownEmployeeId())
                .stream().map(RuleView::from).toList();
    }

    @Transactional
    public List<RuleView> replaceMine(List<RuleCommand> commands) {
        UUID employeeId = ownEmployeeId();
        rules.deleteAllByEmployeeId(employeeId);
        List<AvailabilityRule> saved = commands.stream()
                .map(command -> new AvailabilityRule(employeeId, command.dayOfWeek(), command.startsAt(),
                        command.endsAt(), command.slotMinutes(), command.bufferMinutes()))
                .map(rules::save)
                .toList();
        return saved.stream().map(RuleView::from).toList();
    }

    private UUID ownEmployeeId() {
        return identity.employeeIdForUser(currentUser.id());
    }

    public record Slot(Instant startsAt, Instant endsAt) {
    }

    public record RuleCommand(
            DayOfWeek dayOfWeek,
            LocalTime startsAt,
            LocalTime endsAt,
            int slotMinutes,
            int bufferMinutes) {
    }

    public record RuleView(
            UUID id,
            DayOfWeek dayOfWeek,
            LocalTime startsAt,
            LocalTime endsAt,
            int slotMinutes,
            int bufferMinutes) {
        static RuleView from(AvailabilityRule rule) {
            return new RuleView(rule.getId(), rule.getDayOfWeek(), rule.getStartsAt(), rule.getEndsAt(),
                    rule.getSlotMinutes(), rule.getBufferMinutes());
        }
    }
}
