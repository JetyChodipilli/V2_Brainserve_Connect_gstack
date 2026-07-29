package com.brainserve.appointment.availability.api;

import com.brainserve.appointment.availability.application.AvailabilityService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AvailabilityController {
    private final AvailabilityService service;

    public AvailabilityController(AvailabilityService service) {
        this.service = service;
    }

    @GetMapping("/hosts/{employeeId}/available-slots")
    List<AvailabilityService.Slot> slots(
            @PathVariable UUID employeeId,
            @RequestParam @NotNull LocalDate date) {
        return service.slots(employeeId, date);
    }

    @GetMapping("/employees/me/availability")
    List<AvailabilityService.RuleView> mine() {
        return service.mine();
    }

    @PutMapping("/employees/me/availability")
    List<AvailabilityService.RuleView> replace(@Valid @RequestBody List<RuleRequest> requests) {
        return service.replaceMine(requests.stream().map(RuleRequest::toCommand).toList());
    }

    record RuleRequest(
            @NotNull DayOfWeek dayOfWeek,
            @NotNull LocalTime startsAt,
            @NotNull LocalTime endsAt,
            @Min(10) @Max(240) int slotMinutes,
            @Min(0) @Max(120) int bufferMinutes) {
        AvailabilityService.RuleCommand toCommand() {
            return new AvailabilityService.RuleCommand(dayOfWeek, startsAt, endsAt, slotMinutes, bufferMinutes);
        }
    }
}
