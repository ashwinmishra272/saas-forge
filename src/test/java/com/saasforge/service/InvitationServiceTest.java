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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    @Mock
    private InvitationTokenRepository invitationTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private NotificationProducer notificationProducer;

    @InjectMocks
    private InvitationService invitationService;

    private static final Long TENANT_ID = 1L;
    private static final String ADMIN_EMAIL = "admin@test.com";

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    private void mockSecurityContext(String email) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(email);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private Tenant buildTenant() {
        Tenant tenant = new Tenant();
        tenant.setId(TENANT_ID);
        tenant.setName("Acme Corp");
        return tenant;
    }

    private SystemRole buildRole() {
        SystemRole role = new SystemRole();
        role.setId(2L);
        role.setName("VIEWER");
        role.setRoleKey("VIEWER");
        return role;
    }

    private User buildAdmin() {
        User admin = new User();
        admin.setId(1L);
        admin.setEmail(ADMIN_EMAIL);
        return admin;
    }

    private InviteUserRequest validInviteRequest() {
        InviteUserRequest req = new InviteUserRequest();
        req.setEmail("newuser@test.com");
        req.setRoleId(2L);
        return req;
    }

    private InvitationToken buildValidInvitation() {
        InvitationToken invitation = new InvitationToken();
        invitation.setToken("valid-token");
        invitation.setInvitedEmail("newuser@test.com");
        invitation.setTenant(buildTenant());
        invitation.setRole(buildRole());
        invitation.setInvitedBy(buildAdmin());
        invitation.setExpiresAt(LocalDateTime.now().plusDays(3));
        invitation.setAccepted(false);
        return invitation;
    }

    // ── inviteUser ────────────────────────────────────────────────────────────

    @Test
    void inviteUser_userAlreadyExists_throwsBadRequestException() {
        mockSecurityContext(ADMIN_EMAIL);
        when(userRepository.existsByEmailAndTenantId("newuser@test.com", TENANT_ID)).thenReturn(true);

        assertThatThrownBy(() -> invitationService.inviteUser(validInviteRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("newuser@test.com");
    }

    @Test
    void inviteUser_pendingInvitationExists_throwsBadRequestException() {
        mockSecurityContext(ADMIN_EMAIL);
        when(userRepository.existsByEmailAndTenantId("newuser@test.com", TENANT_ID)).thenReturn(false);
        when(invitationTokenRepository.existsByInvitedEmailAndTenantIdAndAcceptedFalse(
                "newuser@test.com", TENANT_ID)).thenReturn(true);

        assertThatThrownBy(() -> invitationService.inviteUser(validInviteRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("newuser@test.com");
    }

    @Test
    void inviteUser_tenantNotFound_throwsResourceNotFoundException() {
        mockSecurityContext(ADMIN_EMAIL);
        when(userRepository.existsByEmailAndTenantId("newuser@test.com", TENANT_ID)).thenReturn(false);
        when(invitationTokenRepository.existsByInvitedEmailAndTenantIdAndAcceptedFalse(
                "newuser@test.com", TENANT_ID)).thenReturn(false);
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invitationService.inviteUser(validInviteRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Tenant not found");
    }

    @Test
    void inviteUser_roleNotFound_throwsResourceNotFoundException() {
        mockSecurityContext(ADMIN_EMAIL);
        when(userRepository.existsByEmailAndTenantId("newuser@test.com", TENANT_ID)).thenReturn(false);
        when(invitationTokenRepository.existsByInvitedEmailAndTenantIdAndAcceptedFalse(
                "newuser@test.com", TENANT_ID)).thenReturn(false);
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(buildTenant()));
        when(roleRepository.findByIdAndTenantId(2L, TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invitationService.inviteUser(validInviteRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Role not found");
    }

    @Test
    void inviteUser_adminNotFound_throwsResourceNotFoundException() {
        mockSecurityContext(ADMIN_EMAIL);
        when(userRepository.existsByEmailAndTenantId("newuser@test.com", TENANT_ID)).thenReturn(false);
        when(invitationTokenRepository.existsByInvitedEmailAndTenantIdAndAcceptedFalse(
                "newuser@test.com", TENANT_ID)).thenReturn(false);
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(buildTenant()));
        when(roleRepository.findByIdAndTenantId(2L, TENANT_ID)).thenReturn(Optional.of(buildRole()));
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invitationService.inviteUser(validInviteRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Admin user not found");
    }

    @Test
    void inviteUser_success_savesInvitationAndSendsEmail() {
        mockSecurityContext(ADMIN_EMAIL);
        when(userRepository.existsByEmailAndTenantId("newuser@test.com", TENANT_ID)).thenReturn(false);
        when(invitationTokenRepository.existsByInvitedEmailAndTenantIdAndAcceptedFalse(
                "newuser@test.com", TENANT_ID)).thenReturn(false);
        Tenant tenant = buildTenant();
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
        when(roleRepository.findByIdAndTenantId(2L, TENANT_ID)).thenReturn(Optional.of(buildRole()));
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(buildAdmin()));

        invitationService.inviteUser(validInviteRequest());

        ArgumentCaptor<InvitationToken> captor = ArgumentCaptor.forClass(InvitationToken.class);
        verify(invitationTokenRepository).save(captor.capture());
        String savedToken = captor.getValue().getToken();
        verify(notificationProducer).publishInvitation("newuser@test.com", savedToken, tenant.getName());
    }

    @Test
    void inviteUser_setsCorrectExpiryAndAcceptedFalse() {
        mockSecurityContext(ADMIN_EMAIL);
        when(userRepository.existsByEmailAndTenantId("newuser@test.com", TENANT_ID)).thenReturn(false);
        when(invitationTokenRepository.existsByInvitedEmailAndTenantIdAndAcceptedFalse(
                "newuser@test.com", TENANT_ID)).thenReturn(false);
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(buildTenant()));
        when(roleRepository.findByIdAndTenantId(2L, TENANT_ID)).thenReturn(Optional.of(buildRole()));
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(buildAdmin()));

        LocalDateTime before = LocalDateTime.now();
        invitationService.inviteUser(validInviteRequest());

        ArgumentCaptor<InvitationToken> captor = ArgumentCaptor.forClass(InvitationToken.class);
        verify(invitationTokenRepository).save(captor.capture());

        InvitationToken saved = captor.getValue();
        assertThat(saved.isAccepted()).isFalse();
        assertThat(saved.getExpiresAt()).isAfter(before.plusDays(6));
        assertThat(saved.getExpiresAt()).isBefore(before.plusDays(8));
    }

    @Test
    void inviteUser_usesCurrentTenantIdFromContext() {
        mockSecurityContext(ADMIN_EMAIL);
        when(userRepository.existsByEmailAndTenantId("newuser@test.com", TENANT_ID)).thenReturn(false);
        when(invitationTokenRepository.existsByInvitedEmailAndTenantIdAndAcceptedFalse(
                "newuser@test.com", TENANT_ID)).thenReturn(false);
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(buildTenant()));
        when(roleRepository.findByIdAndTenantId(2L, TENANT_ID)).thenReturn(Optional.of(buildRole()));
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(buildAdmin()));

        invitationService.inviteUser(validInviteRequest());

        verify(userRepository).existsByEmailAndTenantId(eq("newuser@test.com"), eq(TENANT_ID));
        verify(invitationTokenRepository).existsByInvitedEmailAndTenantIdAndAcceptedFalse(
                eq("newuser@test.com"), eq(TENANT_ID));
    }

    // ── acceptInvitation ──────────────────────────────────────────────────────

    @Test
    void acceptInvitation_invalidToken_throwsBadRequestException() {
        when(invitationTokenRepository.findByToken("bad-token")).thenReturn(Optional.empty());

        AcceptInvitationRequest request = new AcceptInvitationRequest();
        request.setToken("bad-token");
        request.setName("New User");
        request.setPassword("password123");

        assertThatThrownBy(() -> invitationService.acceptInvitation(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid invitation token");
    }

    @Test
    void acceptInvitation_alreadyAccepted_throwsBadRequestException() {
        InvitationToken accepted = buildValidInvitation();
        accepted.setAccepted(true);
        when(invitationTokenRepository.findByToken("used-token")).thenReturn(Optional.of(accepted));

        AcceptInvitationRequest request = new AcceptInvitationRequest();
        request.setToken("used-token");
        request.setName("New User");
        request.setPassword("password123");

        assertThatThrownBy(() -> invitationService.acceptInvitation(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invitation has already been accepted");
    }

    @Test
    void acceptInvitation_expiredToken_throwsBadRequestException() {
        InvitationToken expired = buildValidInvitation();
        expired.setExpiresAt(LocalDateTime.now().minusHours(1));
        when(invitationTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expired));

        AcceptInvitationRequest request = new AcceptInvitationRequest();
        request.setToken("expired-token");
        request.setName("New User");
        request.setPassword("password123");

        assertThatThrownBy(() -> invitationService.acceptInvitation(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invitation has expired");
    }

    @Test
    void acceptInvitation_emailAlreadyRegistered_throwsBadRequestException() {
        InvitationToken invitation = buildValidInvitation();
        when(invitationTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(invitation));
        when(userRepository.existsByEmailAndTenantId(
                invitation.getInvitedEmail(), invitation.getTenant().getId())).thenReturn(true);

        AcceptInvitationRequest request = new AcceptInvitationRequest();
        request.setToken("valid-token");
        request.setName("New User");
        request.setPassword("password123");

        assertThatThrownBy(() -> invitationService.acceptInvitation(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void acceptInvitation_success_createsUserWithCorrectFields() {
        InvitationToken invitation = buildValidInvitation();
        when(invitationTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(invitation));
        when(userRepository.existsByEmailAndTenantId(
                invitation.getInvitedEmail(), invitation.getTenant().getId())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AcceptInvitationRequest request = new AcceptInvitationRequest();
        request.setToken("valid-token");
        request.setName("New User");
        request.setPassword("password123");

        invitationService.acceptInvitation(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User created = userCaptor.getValue();
        assertThat(created.getName()).isEqualTo("New User");
        assertThat(created.getEmail()).isEqualTo(invitation.getInvitedEmail());
        assertThat(created.getTenant()).isEqualTo(invitation.getTenant());
        assertThat(created.getRole()).isEqualTo(invitation.getRole());
        assertThat(created.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void acceptInvitation_success_encodesPassword() {
        InvitationToken invitation = buildValidInvitation();
        when(invitationTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(invitation));
        when(userRepository.existsByEmailAndTenantId(
                invitation.getInvitedEmail(), invitation.getTenant().getId())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

        AcceptInvitationRequest request = new AcceptInvitationRequest();
        request.setToken("valid-token");
        request.setName("New User");
        request.setPassword("password123");

        invitationService.acceptInvitation(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("encodedPassword");
    }

    @Test
    void acceptInvitation_success_marksInvitationAccepted() {
        InvitationToken invitation = buildValidInvitation();
        when(invitationTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(invitation));
        when(userRepository.existsByEmailAndTenantId(
                invitation.getInvitedEmail(), invitation.getTenant().getId())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");

        AcceptInvitationRequest request = new AcceptInvitationRequest();
        request.setToken("valid-token");
        request.setName("New User");
        request.setPassword("password123");

        invitationService.acceptInvitation(request);

        ArgumentCaptor<InvitationToken> tokenCaptor = ArgumentCaptor.forClass(InvitationToken.class);
        verify(invitationTokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().isAccepted()).isTrue();
    }
}
