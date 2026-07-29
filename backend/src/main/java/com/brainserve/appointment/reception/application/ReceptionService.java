package com.brainserve.appointment.reception.application;

import com.brainserve.appointment.appointment.application.AppointmentService;
import com.brainserve.appointment.employee.application.EmployeeService;
import com.brainserve.appointment.notification.application.OutboxService;
import com.brainserve.appointment.reception.domain.VisitAccessRecord;
import com.brainserve.appointment.reception.domain.VisitorBadge;
import com.brainserve.appointment.reception.infrastructure.AccessRecordRepository;
import com.brainserve.appointment.reception.infrastructure.BadgeRepository;
import com.brainserve.appointment.shared.api.DomainException;
import com.brainserve.appointment.shared.audit.AuditService;
import com.brainserve.appointment.shared.security.CurrentUser;
import com.brainserve.appointment.visitor.application.VisitorService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReceptionService {
    private final AppointmentService appointmentService;
    private final AccessRecordRepository accessRecords;
    private final BadgeRepository badges;
    private final VisitorService visitors;
    private final EmployeeService employees;
    private final CurrentUser currentUser;
    private final AuditService audit;
    private final OutboxService outbox;

    public ReceptionService(
            AppointmentService appointmentService,
            AccessRecordRepository accessRecords,
            BadgeRepository badges,
            VisitorService visitors,
            EmployeeService employees,
            CurrentUser currentUser,
            AuditService audit,
            OutboxService outbox) {
        this.appointmentService = appointmentService;
        this.accessRecords = accessRecords;
        this.badges = badges;
        this.visitors = visitors;
        this.employees = employees;
        this.currentUser = currentUser;
        this.audit = audit;
        this.outbox = outbox;
    }

    @Transactional
    public AccessView checkIn(UUID appointmentId, String gate, String overrideReason) {
        if (accessRecords.existsByAppointmentId(appointmentId)) {
            throw new DomainException("ALREADY_CHECKED_IN", HttpStatus.CONFLICT, "Visitor has already checked in for this appointment.");
        }
        AppointmentService.AccessAppointment appointment = appointmentService.checkInForAccess(appointmentId);
        VisitorService.VisitorRecord visitor = visitors.byId(appointment.visitorId());
        if (visitor.restricted() && (overrideReason == null || overrideReason.isBlank())) {
            throw new DomainException("RESTRICTED_VISITOR", HttpStatus.FORBIDDEN, "Restricted visitor requires an authorized override reason.");
        }
        VisitorBadge badge = badges.findFirstByActiveTrueAndAllocatedFalseOrderByBadgeNumber()
                .orElseThrow(() -> new DomainException("NO_BADGE_AVAILABLE", HttpStatus.CONFLICT, "No visitor badge is available."));
        badge.allocate();
        visitors.markIdentityVerified(visitor.id());
        VisitAccessRecord record = accessRecords.save(new VisitAccessRecord(
                appointmentId, visitor.id(), badge.getId(), gate, currentUser.id(), overrideReason));
        audit.record(currentUser.id(), "CHECK_IN", "VISIT_ACCESS", record.getId(), overrideReason, "CONFIDENTIAL");
        outbox.publish("VISIT_ACCESS", record.getId(), "VisitorCheckedIn",
                "{\"accessRecordId\":\"" + record.getId() + "\",\"appointmentId\":\"" + appointmentId + "\"}");
        return view(record);
    }

    @Transactional
    public AccessView checkOut(UUID recordId, String gate) {
        VisitAccessRecord record = accessRecords.findById(recordId)
                .orElseThrow(() -> new DomainException("ACCESS_RECORD_NOT_FOUND", HttpStatus.NOT_FOUND, "Access record was not found."));
        if (record.getCheckedOutAt() != null) {
            throw new DomainException("ALREADY_CHECKED_OUT", HttpStatus.CONFLICT, "Visitor is already checked out.");
        }
        record.checkOut(gate, currentUser.id());
        badges.findById(record.getBadgeId()).ifPresent(VisitorBadge::release);
        appointmentService.checkOutForAccess(record.getAppointmentId());
        audit.record(currentUser.id(), "CHECK_OUT", "VISIT_ACCESS", recordId, null, "CONFIDENTIAL");
        outbox.publish("VISIT_ACCESS", recordId, "VisitorCheckedOut",
                "{\"accessRecordId\":\"" + recordId + "\"}");
        return view(record);
    }

    @Transactional(readOnly = true)
    public List<AccessView> inside() {
        return accessRecords.findAllByCheckedInAtIsNotNullAndCheckedOutAtIsNullOrderByCheckedInAt()
                .stream().map(this::view).toList();
    }

    private AccessView view(VisitAccessRecord record) {
        VisitorService.VisitorRecord visitor = visitors.byId(record.getVisitorId());
        AppointmentService.AccessAppointment appointment = appointmentService.accessView(record.getAppointmentId());
        EmployeeService.DirectoryEmployee host = employees.directoryById(appointment.hostEmployeeId());
        String badge = badges.findById(record.getBadgeId()).map(VisitorBadge::getBadgeNumber).orElse(null);
        return new AccessView(record.getId(), record.getAppointmentId(), visitor.displayName(),
                visitor.company(), host.displayName(), badge, record.getEntryGate(), record.getExitGate(),
                record.getCheckedInAt(), record.getCheckedOutAt());
    }

    public record AccessView(
            UUID id,
            UUID appointmentId,
            String visitorName,
            String company,
            String hostName,
            String badgeNumber,
            String entryGate,
            String exitGate,
            Instant checkedInAt,
            Instant checkedOutAt) {
    }
}
