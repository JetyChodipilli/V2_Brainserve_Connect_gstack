package com.brainserve.appointment.iam.infrastructure;

import com.brainserve.appointment.iam.domain.UserAccount;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    Optional<UserAccount> findByLoginIgnoreCase(String login);
    boolean existsByLoginIgnoreCase(String login);
}
