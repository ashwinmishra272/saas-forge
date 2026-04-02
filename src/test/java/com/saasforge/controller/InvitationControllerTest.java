package com.saasforge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saasforge.dto.AcceptInvitationRequest;
import com.saasforge.dto.InviteUserRequest;
import com.saasforge.exception.BadRequestException;
import com.saasforge.exception.GlobalExceptionHandler;
import com.saasforge.service.InvitationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class InvitationControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private InvitationService invitationService;

    @InjectMocks
    private InvitationController invitationController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(invitationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private InviteUserRequest validInviteRequest() {
        InviteUserRequest req = new InviteUserRequest();
        req.setEmail("newuser@test.com");
        req.setRoleId(2L);
        return req;
    }

    private AcceptInvitationRequest validAcceptRequest() {
        AcceptInvitationRequest req = new AcceptInvitationRequest();
        req.setToken("valid-invitation-token");
        req.setName("New User");
        req.setPassword("securepassword");
        return req;
    }

    // ── POST /api/invitations/invite ──────────────────────────────────────────

    @Test
    void inviteUser_validRequest_returns200WithToken() throws Exception {
        doNothing().when(invitationService).inviteUser(any());

        mockMvc.perform(post("/api/invitations/invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validInviteRequest())))
                .andExpect(status().isOk())
                .andExpect(content().string("Invitation email sent to newuser@test.com"));

        verify(invitationService).inviteUser(any());
    }

    @Test
    void inviteUser_blankEmail_returns400() throws Exception {
        InviteUserRequest req = validInviteRequest();
        req.setEmail("");

        mockMvc.perform(post("/api/invitations/invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void inviteUser_invalidEmail_returns400() throws Exception {
        InviteUserRequest req = validInviteRequest();
        req.setEmail("not-an-email");

        mockMvc.perform(post("/api/invitations/invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void inviteUser_nullRoleId_returns400() throws Exception {
        InviteUserRequest req = validInviteRequest();
        req.setRoleId(null);

        mockMvc.perform(post("/api/invitations/invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void inviteUser_userAlreadyExists_returns400() throws Exception {
        doThrow(new BadRequestException("User with email 'newuser@test.com' already exists in this tenant"))
                .when(invitationService).inviteUser(any());

        mockMvc.perform(post("/api/invitations/invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validInviteRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("User with email 'newuser@test.com' already exists in this tenant"));
    }

    @Test
    void inviteUser_pendingInvitationExists_returns400() throws Exception {
        doThrow(new BadRequestException("A pending invitation already exists for: newuser@test.com"))
                .when(invitationService).inviteUser(any());

        mockMvc.perform(post("/api/invitations/invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validInviteRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("A pending invitation already exists for: newuser@test.com"));
    }

    // ── POST /api/invitations/accept ──────────────────────────────────────────

    @Test
    void acceptInvitation_validRequest_returns200() throws Exception {
        doNothing().when(invitationService).acceptInvitation(any());

        mockMvc.perform(post("/api/invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validAcceptRequest())))
                .andExpect(status().isOk())
                .andExpect(content().string("Invitation accepted. You can now log in."));

        verify(invitationService).acceptInvitation(any());
    }

    @Test
    void acceptInvitation_blankToken_returns400() throws Exception {
        AcceptInvitationRequest req = validAcceptRequest();
        req.setToken("");

        mockMvc.perform(post("/api/invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void acceptInvitation_blankName_returns400() throws Exception {
        AcceptInvitationRequest req = validAcceptRequest();
        req.setName("");

        mockMvc.perform(post("/api/invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void acceptInvitation_shortPassword_returns400() throws Exception {
        AcceptInvitationRequest req = validAcceptRequest();
        req.setPassword("short");

        mockMvc.perform(post("/api/invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void acceptInvitation_invalidToken_returns400() throws Exception {
        doThrow(new BadRequestException("Invalid invitation token"))
                .when(invitationService).acceptInvitation(any());

        mockMvc.perform(post("/api/invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validAcceptRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid invitation token"));
    }

    @Test
    void acceptInvitation_expiredToken_returns400() throws Exception {
        doThrow(new BadRequestException("Invitation has expired"))
                .when(invitationService).acceptInvitation(any());

        mockMvc.perform(post("/api/invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validAcceptRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invitation has expired"));
    }

    @Test
    void acceptInvitation_alreadyAccepted_returns400() throws Exception {
        doThrow(new BadRequestException("Invitation has already been accepted"))
                .when(invitationService).acceptInvitation(any());

        mockMvc.perform(post("/api/invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validAcceptRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invitation has already been accepted"));
    }
}
