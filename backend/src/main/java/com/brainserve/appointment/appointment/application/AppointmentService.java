package com.brainserve.appointment.appointment.application;

import com.brainserve.appointment.appointment.domain.Appointment;
import com.brainserve.appointment.appointment.domain.AppointmentStatus;
import com.brainserve.appointment.appointment.infrastructure.AppointmentRepository;
import com.brainserve.appointment.employee.application.EmployeeService;
import com.brainserve.appointment.iam.application.IdentityDirectory;
import com.brainserve.appointment.notification.application.OutboxService;
import com.brainserve.appointment.shared.api.DomainException;
import com.brainserve.appointment.shared.audit.AuditService;
import com.brainserve.appointment.shared.security.CurrentUser;
import com.brainserve.appointment.visitor.application.VisitorService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentService {
    private static final Set<String> TYPES = Set.of(
            "EMPLOYEE_VISIT", "HR_VISIT", "CEO_VISIT", "INTERVIEW", "VENDOR_VISIT",
            "CLIENT_MEETING", "SERVICE_VISIT", "DELIVERY", "OTHER");
    private final AppointmentRepository appointments;
    private final VisitorService visitors;
    private final EmployeeService employees;
    private final IdentityDirectory identity;
    private final CurrentUser currentUser;
    private final AuditService audit;
    private final OutboxService outbox;
    private final SecureRandom random = new SecureRandom();
    private final boolean revealOtp;

    public AppointmentService(
            AppointmentRepository appointments,
            VisitorService visitors,
            EmployeeService employees,
            IdentityDirectory identity,
            CurrentUser currentUser,
            AuditService audit,
            OutboxService outbox,
            @Value("${brainserve.development.reveal-otp:false}") boolean revealOtp) {
        this.appointments = appointments;
        this.visitors = visitors;
        this.employees = employees;
        this.identity = identity;
        this.currentUser = currentUser;
        this.audit = audit;
        this.outbox = outbox;
        this.revealOtp = revealOtp;
    }

    @Transactional
    public BookingResult request(String idempotencyKey, BookingCommand command) {
        if (idempotencyKey != null) {
            var existing = appointments.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) return BookingResult.from(existing.get(), null);
        }
        EmployeeService.DirectoryEmployee host = employees.publicHost(command.hostEmployeeId());
        if (!TYPES.contains(command.type().toUpperCase(Locale.ROOT))) {
            throw new DomainException("APPOINTMENT_TYPE_INVALID", HttpStatus.BAD_REQUEST, "Appointment type is invalid.");
        }
        if (command.startsAt().isBefore(Instant.now().plusSeconds(300))) {
            throw new DomainException("SLOT_IN_PAST", HttpStatus.BAD_REQUEST, "Select a future appointment slot.");
        }
        VisitorService.VisitorRecord visitor = visitors.register(
                command.visitor().firstName(),
                command.visitor().lastName(),
                command.visitor().email(),
                command.visitor().phone(),
                command.visitor().company(),
                command.visitor().consentVersion());
        String code = String.format("%06d", random.nextInt(1_000_000));
        Appointment appointment = appointments.save(new Appointment(
                trackingReference(),
                idempotencyKey,
                visitor.id(),
                host.id(),
                command.type().toUpperCase(Locale.ROOT),
                command.purpose().trim(),
                command.startsAt(),
                command.endsAt(),
                hash(code)));
        outbox.publish("APPOINTMENT", appointment.getId(), "AppointmentVerificationRequested",
                "{\"appointmentId\":\"" + appointment.getId() + "\",\"visitorId\":\"" + visitor.id() + "\"}");
        return BookingResult.from(appointment, revealOtp ? code : null);
    }

    @Transactional
    public AppointmentView verify(String reference, String otp) {
        Appointment appointment = byReference(reference);
        if (appointment.getCreatedAt().isBefore(Instant.now().minusSeconds(600))) {
            throw new DomainException("OTP_EXPIRED", HttpStatus.GONE, "Verification code has expired.");
        }
        if (!MessageDigest.isEqual(
                hash(otp).getBytes(StandardCharsets.US_ASCII),
                String.valueOf(appointment.getVerificationHash()).getBytes(StandardCharsets.US_ASCII))) {
            throw new DomainException("OTP_INVALID", HttpStatus.BAD_REQUEST, "Verification code is invalid or expired.");
        }
        appointment.verify();
        visitors.markOtpVerified(appointment.getVisitorId());
        outbox.publish("APPOINTMENT", appointment.getId(), "AppointmentRequested",
                "{\"appointmentId\":\"" + appointment.getId() + "\",\"hostId\":\"" + appointment.getHostEmployeeId() + "\"}");
        return view(appointment);
    }

    @Transactional(readOnly = true)
    public PublicAppointmentView track(String reference) {
        Appointment appointment = byReference(reference);
        EmployeeService.DirectoryEmployee host = employees.directoryById(appointment.getHostEmployeeId());
        return new PublicAppointmentView(appointment.getReferenceNumber(), appointment.getStatus(),
                appointment.getType(), appointment.getStartsAt(), appointment.getEndsAt(), host.displayName());
    }

    @Transactional(readOnly = true)
    public List<AppointmentView> myQueue() {
        if (currentUser.has("ROLE_HR_ADMIN")) {
            return appointments.findAll().stream()
                    .filter(item -> Set.of("HR_VISIT", "INTERVIEW").contains(item.getType()))
                    .map(this::view).toList();
        }
        UUID employeeId = identity.optionalEmployeeIdForUser(currentUser.id());
        if (employeeId == null) return List.of();
        return appointments.findAllByHostEmployeeIdAndStatusInOrderByStartsAt(
                employeeId,
                List.of(AppointmentStatus.PENDING_APPROVAL, AppointmentStatus.APPROVED))
                .stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public List<AppointmentView> approvedArrivals() {
        return appointments.findAllByStatusOrderByStartsAt(AppointmentStatus.APPROVED)
                .stream().map(this::view).toList();
    }

    @Transactional
    public AppointmentView decide(UUID appointmentId, boolean approve, String remarks) {
        Appointment appointment = appointments.findLockedById(appointmentId)
                .orElseThrow(() -> new DomainException("APPOINTMENT_NOT_FOUND", HttpStatus.NOT_FOUND, "Appointment was not found."));
        boolean owns = appointment.getHostEmployeeId().equals(identity.optionalEmployeeIdForUser(currentUser.id()));
        boolean hrFlow = currentUser.has("ROLE_HR_ADMIN")
                && Set.of("HR_VISIT", "INTERVIEW").contains(appointment.getType());
        boolean ceoDelegate = appointment.getType().equals("CEO_VISIT") && currentUser.has("CEO_APPOINTMENT_APPROVE");
        if (!owns && !hrFlow && !ceoDelegate) {
            throw new DomainException("APPOINTMENT_NOT_ASSIGNED", HttpStatus.FORBIDDEN, "Only the assigned approver can decide.");
        }
        AppointmentStatus target = approve ? AppointmentStatus.APPROVED : AppointmentStatus.REJECTED;
        appointment.decide(target, currentUser.id(), remarks);
        audit.record(currentUser.id(), approve ? "APPROVE" : "REJECT", "APPOINTMENT", appointmentId, remarks, "CONFIDENTIAL");
        outbox.publish("APPOINTMENT", appointmentId, approve ? "AppointmentApproved" : "AppointmentRejected",
                "{\"appointmentId\":\"" + appointmentId + "\"}");
        return view(appointment);
    }

    @Transactional
    public AccessAppointment checkInForAccess(UUID id) {
        Appointment appointment = appointments.findLockedById(id)
                .orElseThrow(() -> new DomainException("APPOINTMENT_NOT_FOUND", HttpStatus.NOT_FOUND, "Appointment was not found."));
        if (appointment.getStatus() != AppointmentStatus.APPROVED) {
            throw new DomainException("APPOINTMENT_NOT_APPROVED", HttpStatus.CONFLICT, "Only approved appointments can check in.");
        }
        appointment.transition(AppointmentStatus.CHECKED_IN);
        return access(appointment);
    }

    @Transactional
    public AccessAppointment checkOutForAccess(UUID id) {
        Appointment appointment = appointments.findLockedById(id)
                .orElseThrow(() -> new DomainException("APPOINTMENT_NOT_FOUND", HttpStatus.NOT_FOUND, "Appointment was not found."));
        appointment.transition(AppointmentStatus.CHECKED_OUT);
        return access(appointment);
    }

    @Transactional(readOnly = true)
    public AccessAppointment accessView(UUID id) {
        return appointments.findById(id).map(this::access)
                .orElseThrow(() -> new DomainException("APPOINTMENT_NOT_FOUND", HttpStatus.NOT_FOUND, "Appointment was not found."));
    }

    @Transactional(readOnly = true)
    public boolean slotTaken(UUID hostEmployeeId, Instant startsAt) {
        return appointments.existsByHostEmployeeIdAndStartsAtAndStatusIn(
                hostEmployeeId,
                startsAt,
                List.of(
                        AppointmentStatus.PENDING_VERIFICATION,
                        AppointmentStatus.PENDING_APPROVAL,
                        AppointmentStatus.APPROVED,
                        AppointmentStatus.CHECKED_IN,
                        AppointmentStatus.IN_MEETING));
    }

    private Appointment byReference(String reference) {
        return appointments.findByReferenceNumber(reference.toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new DomainException("APPOINTMENT_NOT_FOUND", HttpStatus.NOT_FOUND, "Appointment was not found."));
    }

    private String trackingReference() {
        byte[] bytes = new byte[7];
        random.nextBytes(bytes);
        return "BSA-" + HexFormat.of().withUpperCase().formatHex(bytes);
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private AppointmentView view(Appointment appointment) {
        VisitorService.VisitorRecord visitor = visitors.byId(appointment.getVisitorId());
        EmployeeService.DirectoryEmployee host = employees.directoryById(appointment.getHostEmployeeId());
        return new AppointmentView(
                appointment.getId(), appointment.getReferenceNumber(), appointment.getStatus(),
                appointment.getType(), appointment.getPurpose(), appointment.getStartsAt(), appointment.getEndsAt(),
                new VisitorSummary(visitor.id(), visitor.displayName(), visitor.company(),
                        maskEmail(visitor.email()), visitor.verificationStatus(), visitor.restricted()),
                new HostSummary(host.id(), host.displayName(), host.employeeNumber()),
                appointment.getDecisionAt(), appointment.getDecisionRemarks(), appointment.getVersion());
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        return at <= 1 ? "***" + email.substring(Math.max(at, 0)) : email.charAt(0) + "***" + email.substring(at);
    }

    private AccessAppointment access(Appointment appointment) {
        return new AccessAppointment(
                appointment.getId(), appointment.getVisitorId(), appointment.getHostEmployeeId(),
                appointment.getStatus().name(), appointment.getReferenceNumber());
    }

    public record VisitorInput(
            String firstName,
            String lastName,
            String email,
            String phone,
            String company,
            String consentVersion) {
    }

    public record BookingCommand(
            UUID hostEmployeeId,
            String type,
            Instant startsAt,
            Instant endsAt,
            String purpose,
            VisitorInput visitor) {
    }

    public record BookingResult(
            String referenceNumber,
            AppointmentStatus status,
            Instant startsAt,
            boolean verificationRequired,
            String developmentVerificationCode) {
        static BookingResult from(Appointment appointment, String code) {
            return new BookingResult(appointment.getReferenceNumber(), appointment.getStatus(),
                    appointment.getStartsAt(), appointment.getStatus() == AppointmentStatus.PENDING_VERIFICATION, code);
        }
    }

    public record VisitorSummary(
            UUID id, String displayName, String company, String maskedEmail, String verificationStatus, boolean restricted) {
    }

    public record HostSummary(UUID id, String displayName, String employeeNumber) {
    }

    public record AppointmentView(
            UUID id,
            String referenceNumber,
            AppointmentStatus status,
            String type,
            String purpose,
            Instant startsAt,
            Instant endsAt,
            VisitorSummary visitor,
            HostSummary host,
            Instant decisionAt,
            String decisionRemarks,
            long version) {
    }

    public record PublicAppointmentView(
            String referenceNumber, AppointmentStatus status, String type, Instant startsAt, Instant endsAt, String hostName) {
    }

    public record AccessAppointment(
            UUID id,
            UUID visitorId,
            UUID hostEmployeeId,
            String status,
            String referenceNumber) {
    }
}
