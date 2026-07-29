package com.brainserve.appointment.compensation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.brainserve.appointment.compensation.domain.CompensationPackage;
import com.brainserve.appointment.shared.api.DomainException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CompensationPackageTest {

    @Test
    void calculatesMoneyAndAllowsAnIndependentApprover() {
        UUID maker = UUID.randomUUID();
        UUID checker = UUID.randomUUID();
        CompensationPackage item = packageProposedBy(maker);

        item.approve(checker);

        assertThat(item.getGross()).isEqualByComparingTo("75000.00");
        assertThat(item.getNet()).isEqualByComparingTo("70000.00");
        assertThat(item.getAnnualCtc()).isEqualByComparingTo("900000.00");
        assertThat(item.getStatus()).isEqualTo("APPROVED");
        assertThat(item.getApprovedBy()).isEqualTo(checker);
    }

    @Test
    void preventsTheMakerFromApprovingTheirOwnRequest() {
        UUID maker = UUID.randomUUID();
        CompensationPackage item = packageProposedBy(maker);

        assertThatThrownBy(() -> item.approve(maker))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Proposer cannot approve");
    }

    private CompensationPackage packageProposedBy(UUID maker) {
        return new CompensationPackage(
                UUID.randomUUID(),
                new BigDecimal("50000"),
                new BigDecimal("20000"),
                new BigDecimal("5000"),
                new BigDecimal("5000"),
                "INR",
                LocalDate.of(2026, 8, 1),
                null,
                maker);
    }
}
