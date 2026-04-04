package com.saasforge.dto;

public record NotificationEvent(
        String type,
        String toEmail,
        String token,
        String tenantName
) {
    public static final String PASSWORD_RESET = "PASSWORD_RESET";
    public static final String INVITATION_SENT = "INVITATION_SENT";
}
