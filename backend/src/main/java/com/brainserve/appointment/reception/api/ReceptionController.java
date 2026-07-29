package com.brainserve.appointment.reception.api;

import com.brainserve.appointment.appointment.application.AppointmentService;
import com.brainserve.appointment.reception.application.ReceptionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reception")
public class ReceptionController {
    private final ReceptionService service;
    private final AppointmentService appointments;

    public ReceptionController(ReceptionService service, AppointmentService appointments) {
        this.service = service;
        this.appointments = appointments;
    }

    @GetMapping("/appointments/arrivals")
    @PreAuthorize("hasAuthority('VISITOR_CHECK_IN')")
    List<AppointmentService.AppointmentView> approvedArrivals() {
        return appointments.approvedArrivals();
    }

    @PostMapping("/appointments/{appointmentId}/check-in")
    @PreAuthorize("hasAuthority('VISITOR_CHECK_IN')")
    ReceptionService.AccessView checkIn(
            @PathVariable UUID appointmentId,
            @Valid @RequestBody CheckInRequest request) {
        return service.checkIn(appointmentId, request.gate(), request.overrideReason());
    }

    @PostMapping("/access-records/{recordId}/check-out")
    @PreAuthorize("hasAuthority('VISITOR_CHECK_OUT')")
    ReceptionService.AccessView checkOut(
            @PathVariable UUID recordId,
            @Valid @RequestBody CheckOutRequest request) {
        return service.checkOut(recordId, request.gate());
    }

    @GetMapping("/visitors-inside")
    @PreAuthorize("hasAnyAuthority('VISITOR_CHECK_IN','VISITOR_CHECK_OUT','REPORT_VIEW')")
    List<ReceptionService.AccessView> inside() {
        return service.inside();
    }

    @GetMapping("/emergency-list")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    List<ReceptionService.AccessView> emergencyList() {
        return service.inside();
    }

    record CheckInRequest(
            @NotBlank @Size(max = 80) String gate,
            @Size(max = 500) String overrideReason) {
    }

    record CheckOutRequest(@NotBlank @Size(max = 80) String gate) {
    }
}
