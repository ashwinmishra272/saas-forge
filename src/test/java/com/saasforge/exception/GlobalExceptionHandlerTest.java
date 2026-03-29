package com.saasforge.exception;

import com.saasforge.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    // ── ResourceNotFoundException ──────────────────────────────────────────────

    @Test
    void handleNotFound_returns404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User not found");

        ResponseEntity<ErrorResponse> response = handler.handleNotFound(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void handleNotFound_bodyContainsMessage() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User not found");

        ResponseEntity<ErrorResponse> response = handler.handleNotFound(ex, request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("User not found");
    }

    @Test
    void handleNotFound_bodyContainsPath() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User not found");

        ResponseEntity<ErrorResponse> response = handler.handleNotFound(ex, request);

        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
    }

    @Test
    void handleNotFound_bodyContainsStatusCode() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User not found");

        ResponseEntity<ErrorResponse> response = handler.handleNotFound(ex, request);

        assertThat(response.getBody().getStatus()).isEqualTo(404);
    }

    // ── BadRequestException ────────────────────────────────────────────────────

    @Test
    void handleBadRequest_returns400() {
        BadRequestException ex = new BadRequestException("Invalid input");

        ResponseEntity<ErrorResponse> response = handler.handleBadRequest(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleBadRequest_bodyContainsMessage() {
        BadRequestException ex = new BadRequestException("Invalid input");

        ResponseEntity<ErrorResponse> response = handler.handleBadRequest(ex, request);

        assertThat(response.getBody().getError()).isEqualTo("Invalid input");
    }

    @Test
    void handleBadRequest_bodyContainsPath() {
        BadRequestException ex = new BadRequestException("Invalid input");

        ResponseEntity<ErrorResponse> response = handler.handleBadRequest(ex, request);

        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
    }

    // ── MethodArgumentNotValidException ───────────────────────────────────────

    @Test
    void handleValidationErrors_returns400() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(
                List.of(new FieldError("obj", "email", "must not be blank"))
        );

        ResponseEntity<ValidationErrorResponse> response = handler.handleValidationErrors(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleValidationErrors_bodyContainsFieldErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(
                List.of(new FieldError("obj", "email", "must not be blank"))
        );

        ResponseEntity<ValidationErrorResponse> response = handler.handleValidationErrors(ex, request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrors()).containsEntry("email", "must not be blank");
    }

    @Test
    void handleValidationErrors_bodyContainsValidationFailedMessage() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        ResponseEntity<ValidationErrorResponse> response = handler.handleValidationErrors(ex, request);

        assertThat(response.getBody().getError()).isEqualTo("Validation failed");
    }

    @Test
    void handleValidationErrors_multipleFieldErrors_allIncluded() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("obj", "email", "must not be blank"),
                new FieldError("obj", "name", "must not be blank")
        ));

        ResponseEntity<ValidationErrorResponse> response = handler.handleValidationErrors(ex, request);

        assertThat(response.getBody().getErrors()).hasSize(2);
        assertThat(response.getBody().getErrors()).containsKey("email");
        assertThat(response.getBody().getErrors()).containsKey("name");
    }

    // ── AccessDeniedException ─────────────────────────────────────────────────

    @Test
    void handleAccessDenied_returns403() {
        AccessDeniedException ex = new AccessDeniedException("Access denied");

        ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void handleAccessDenied_bodyContainsForbiddenMessage() {
        AccessDeniedException ex = new AccessDeniedException("Access denied");

        ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(ex, request);

        assertThat(response.getBody().getError())
                .isEqualTo("You do not have permission to perform this action");
    }

    @Test
    void handleAccessDenied_bodyContainsPath() {
        AccessDeniedException ex = new AccessDeniedException("Access denied");

        ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(ex, request);

        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
    }

    // ── Generic Exception ─────────────────────────────────────────────────────

    @Test
    void handleGeneric_returns500() {
        Exception ex = new RuntimeException("Unexpected error");

        ResponseEntity<ErrorResponse> response = handler.handleGeneric(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void handleGeneric_bodyContainsSafeMessage() {
        Exception ex = new RuntimeException("Sensitive internal detail");

        ResponseEntity<ErrorResponse> response = handler.handleGeneric(ex, request);

        assertThat(response.getBody().getError()).isEqualTo("Something went wrong");
    }

    @Test
    void handleGeneric_doesNotExposeExceptionMessage() {
        Exception ex = new RuntimeException("Sensitive internal detail");

        ResponseEntity<ErrorResponse> response = handler.handleGeneric(ex, request);

        assertThat(response.getBody().getError()).doesNotContain("Sensitive internal detail");
    }
}
