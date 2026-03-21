package com.saasforge.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@AllArgsConstructor
public class ValidationErrorResponse {

    private int status;
    private String error;
    private String path;
    private LocalDateTime timestamp;
    private Map<String, String> errors;
}
