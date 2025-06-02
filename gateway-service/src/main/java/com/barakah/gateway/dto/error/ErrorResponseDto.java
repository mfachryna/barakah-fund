package com.barakah.gateway.dto.error;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponseDto {
    private String error;
    private String message;
    private String errorCode;
    private Integer status;
    private String path;
    private LocalDateTime timestamp;
    private List<FieldErrorDto> fieldErrors;
    private String traceId;
}