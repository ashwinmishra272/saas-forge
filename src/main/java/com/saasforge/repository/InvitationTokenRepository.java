package com.saasforge.repository;

import com.saasforge.entity.InvitationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface InvitationTokenRepository extends JpaRepository<InvitationToken, Long> {

    Optional<InvitationToken> findByToken(String token);

    boolean existsByInvitedEmailAndTenantIdAndAcceptedFalse(String email, Long tenantId);

    @Modifying
    @Query("UPDATE InvitationToken i SET i.accepted = true WHERE i.invitedEmail = :email AND i.tenant.id = :tenantId")
    void invalidatePendingInvitations(String email, Long tenantId);
}
