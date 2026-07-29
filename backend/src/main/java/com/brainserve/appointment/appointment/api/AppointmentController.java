package com.brainserve.appointment.appointment.api;

import com.brainserve.appointment.appointment.application.AppointmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AppointmentController {
    private final AppointmentService service;

    public AppointmentController(AppointmentService service) {
        this.service = service;
    }

    @PostMapping("/public/appointments")
    AppointmentService.BookingResult request(
            @RequestHeader(value = "Idempotency-Key", required = false) @Size(max = 100) String idempotencyKey,
            @Valid @RequestBody BookingRequest request) {
        return service.request(idempotencyKey, request.toCommand());
    }

    @PostMapping("/public/appointments/{reference}/verify-otp")
    AppointmentService.AppointmentView verify(
            @PathVariable String reference,
            @Valid @RequestBody VerifyRequest request) {
        return service.verify(reference, request.otp());
    }

    @GetMapping("/public/appointments/{reference}")
    AppointmentService.PublicAppointmentView track(@PathVariable String reference) {
        return service.track(reference);
    }

    @GetMapping("/appointments")
    @PreAuthorize("hasAnyAuthority('APPOINTMENT_APPROVE','APPOINTMENT_REJECT')")
    List<AppointmentService.AppointmentView> queue() {
        return service.myQueue();
    }

    @PostMapping("/appointments/{id}/approve")
    @PreAuthorize("hasAuthority('APPOINTMENT_APPROVE') or hasAuthority('CEO_APPOINTMENT_APPROVE')")
    AppointmentService.AppointmentView approve(@PathVariable UUID id, @RequestBody DecisionRequest request) {
        return service.decide(id, true, request.remarks());
    }

    @PostMapping("/appointments/{id}/reject")
    @PreAuthorize("hasAuthority('APPOINTMENT_REJECT')")
    AppointmentService.AppointmentView reject(@PathVariable UUID id, @RequestBody DecisionRequest request) {
        return service.decide(id, false, request.remarks());
    }

    record BookingRequest(
            @NotNull UUID hostEmployeeId,
            @NotBlank String type,
            @NotNull Instant startsAt,
            @NotNull Instant endsAt,
            @NotBlank @Size(min = 5, max = 500) String purpose,
            @NotNull @Valid VisitorRequest visitor) {
        AppointmentService.BookingCommand toCommand() {
            return new AppointmentService.BookingCommand(hostEmployeeId, type, startsAt, endsAt, purpose,
                    new AppointmentService.VisitorInput(visitor.firstName(), visitor.lastName(), visitor.email(),
                            visitor.phone(), visitor.company(), visitor.consentVersion()));
        }
    }

    record VisitorRequest(
            @NotBlank @Size(max = 80) String firstName,
            @NotBlank @Size(max = 80) String lastName,
            @NotBlank @Email String email,
            @NotBlank @Pattern(regexp = "[+0-9() -]{8,24}") String phone,
            @Size(max = 180) String company,
            @NotBlank @Size(max = 40) String consentVersion) {
    }

    record VerifyRequest(@NotBlank @Pattern(regexp = "\\d{6}") String otp) {
    }

    record DecisionRequest(@Size(max = 500) String remarks) {
    }
}
