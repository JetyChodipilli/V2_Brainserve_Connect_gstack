package com.brainserve.appointment.visitor.application;

import com.brainserve.appointment.shared.api.DomainException;
import com.brainserve.appointment.visitor.domain.Visitor;
import com.brainserve.appointment.visitor.infrastructure.VisitorRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VisitorService {
    private final VisitorRepository visitors;

    public VisitorService(VisitorRepository visitors) {
        this.visitors = visitors;
    }

    @Transactional
    public VisitorRecord register(
            String firstName,
            String lastName,
            String email,
            String phone,
            String company,
            String consentVersion) {
        return record(visitors.save(new Visitor(firstName, lastName, email, phone, company, consentVersion)));
    }

    @Transactional
    public void markOtpVerified(UUID visitorId) {
        find(visitorId).otpVerified();
    }

    @Transactional
    public void markIdentityVerified(UUID visitorId) {
        find(visitorId).identityVerified();
    }

    @Transactional(readOnly = true)
    public VisitorRecord byId(UUID visitorId) {
        return record(find(visitorId));
    }

    private Visitor find(UUID id) {
        return visitors.findById(id)
                .orElseThrow(() -> new DomainException("VISITOR_NOT_FOUND", HttpStatus.NOT_FOUND, "Visitor was not found."));
    }

    private VisitorRecord record(Visitor visitor) {
        return new VisitorRecord(
                visitor.getId(),
                visitor.getDisplayName(),
                visitor.getEmail(),
                visitor.getPhone(),
                visitor.getCompany(),
                visitor.getVerificationStatus(),
                visitor.isRestricted(),
                visitor.getRestrictionReason());
    }

    public record VisitorRecord(
            UUID id,
            String displayName,
            String email,
            String phone,
            String company,
            String verificationStatus,
            boolean restricted,
            String restrictionReason) {
    }
}
