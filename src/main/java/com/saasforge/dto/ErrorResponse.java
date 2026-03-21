package com.saasforge.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ErrorResponse {

    private int status;
    private String error;
    private String path;
    private LocalDateTime timestamp;
}
