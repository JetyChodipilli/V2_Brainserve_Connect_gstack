package com.brainserve.appointment.iam.infrastructure;

import com.brainserve.appointment.iam.domain.RefreshTokenSession;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenSession, UUID> {
    Optional<RefreshTokenSession> findByTokenHash(String tokenHash);
    List<RefreshTokenSession> findAllByUserIdAndRevokedAtIsNull(UUID userId);

    @Modifying
    @Query("update RefreshTokenSession r set r.revokedAt = current_timestamp where r.familyId = :familyId and r.revokedAt is null")
    int revokeFamily(UUID familyId);
}
