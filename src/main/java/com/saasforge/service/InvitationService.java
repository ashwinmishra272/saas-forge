package com.saasforge.service;

import com.saasforge.dto.AcceptInvitationRequest;
import com.saasforge.dto.InviteUserRequest;
import com.saasforge.entity.InvitationToken;
import com.saasforge.entity.SystemRole;
import com.saasforge.entity.Tenant;
import com.saasforge.entity.User;
import com.saasforge.exception.BadRequestException;
import com.saasforge.exception.ResourceNotFoundException;
import com.saasforge.repository.InvitationTokenRepository;
import com.saasforge.repository.RoleRepository;
import com.saasforge.repository.TenantRepository;
import com.saasforge.repository.UserRepository;
import com.saasforge.security.TenantContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvitationService {

    private final InvitationTokenRepository invitationTokenRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    public String inviteUser(InviteUserRequest request) {
        Long tenantId = TenantContext.getCurrentTenantId();

        // Get currently logged in admin
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String adminEmail = (String) authentication.getPrincipal();

        log.info("Admin {} inviting {} to tenantId={}", adminEmail, request.getEmail(), tenantId);

        // Check if email is already a registered user in this tenant
        if (userRepository.existsByEmailAndTenantId(request.getEmail(), tenantId)) {
            throw new BadRequestException("User with email '" + request.getEmail() + "' already exists in this tenant");
        }

        // Check if there is already a pending invitation for this email
        if (invitationTokenRepository.existsByInvitedEmailAndTenantIdAndAcceptedFalse(
                request.getEmail(), tenantId)) {
            throw new BadRequestException("A pending invitation already exists for: " + request.getEmail());
        }

        // Fetch tenant and role
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        SystemRole role = roleRepository.findByIdAndTenantId(request.getRoleId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + request.getRoleId()));

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));

        // Create invitation token
        String tokenValue = UUID.randomUUID().toString();

        InvitationToken invitation = new InvitationToken();
        invitation.setToken(tokenValue);
        invitation.setInvitedEmail(request.getEmail());
        invitation.setTenant(tenant);
        invitation.setRole(role);
        invitation.setInvitedBy(admin);
        invitation.setExpiresAt(LocalDateTime.now().plusDays(7));
        invitation.setAccepted(false);

        invitationTokenRepository.save(invitation);

        log.info("Invitation created for {} in tenantId={}", request.getEmail(), tenantId);

        // In production — send this token via email as a link
        // e.g. https://yourapp.com/accept-invite?token=abc123
        return tokenValue;
    }

    @Transactional
    public void acceptInvitation(AcceptInvitationRequest request) {
        log.info("Accepting invitation with token");

        InvitationToken invitation = invitationTokenRepository
                .findByToken(request.getToken())
                .orElseThrow(() -> new BadRequestException("Invalid invitation token"));

        if (invitation.isAccepted()) {
            log.warn("Attempt to reuse accepted invitation token");
            throw new BadRequestException("Invitation has already been accepted");
        }

        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Expired invitation token used for email={}", invitation.getInvitedEmail());
            throw new BadRequestException("Invitation has expired");
        }

        // Check if email was already registered (edge case — registered between invite and accept)
        if (userRepository.existsByEmailAndTenantId(
                invitation.getInvitedEmail(), invitation.getTenant().getId())) {
            throw new BadRequestException("An account with this email already exists");
        }

        // Create the user account
        User newUser = new User();
        newUser.setName(request.getName());
        newUser.setEmail(invitation.getInvitedEmail());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setTenant(invitation.getTenant());
        newUser.setRole(invitation.getRole());
        newUser.setStatus("ACTIVE");

        userRepository.save(newUser);

        // Mark invitation as accepted
        invitation.setAccepted(true);
        invitationTokenRepository.save(invitation);

        log.info("Invitation accepted — user created for email={}", invitation.getInvitedEmail());
    }
}
