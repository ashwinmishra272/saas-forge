package com.saasforge.controller;

import com.saasforge.dto.AcceptInvitationRequest;
import com.saasforge.dto.InviteUserRequest;
import com.saasforge.service.InvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
@Tag(name = "Invitations", description = "User invitation management")
public class InvitationController {

    private final InvitationService invitationService;

    @PostMapping("/invite")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Invite user", description = "Admin sends an invitation to a new user by email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invitation sent"),
            @ApiResponse(responseCode = "400", description = "User already exists or pending invitation exists"),
            @ApiResponse(responseCode = "403", description = "Admin access required")
    })
    public ResponseEntity<String> inviteUser(@Valid @RequestBody InviteUserRequest request) {
        invitationService.inviteUser(request);
        return ResponseEntity.ok("Invitation email sent to " + request.getEmail());
    }

    @PostMapping("/accept")
    @Operation(summary = "Accept invitation", description = "Invited user accepts the invitation and sets their password")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invitation accepted, account created"),
            @ApiResponse(responseCode = "400", description = "Invalid, expired, or already accepted token")
    })
    public ResponseEntity<String> acceptInvitation(@Valid @RequestBody AcceptInvitationRequest request) {
        invitationService.acceptInvitation(request);
        return ResponseEntity.ok("Invitation accepted. You can now log in.");
    }
}
