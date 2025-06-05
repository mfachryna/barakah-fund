package com.barakah.gateway.exception;

import com.barakah.gateway.dto.common.ErrorResponse;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(StatusRuntimeException.class)
    public ResponseEntity<ErrorResponse> handleGrpcException(
            StatusRuntimeException ex, WebRequest request) {

        Status.Code code = ex.getStatus().getCode();
        HttpStatus httpStatus = mapGrpcStatusToHttp(code);

        ErrorResponse errorResponse = ErrorResponse.fromGrpc(
                httpStatus.value(),
                httpStatus.getReasonPhrase(),
                ex.getStatus().getDescription() != null
                ? ex.getStatus().getDescription() : "Service error occurred",
                code.name()
        );

        errorResponse.setPath(getPath(request));

        if (isClientError(code)) {
            log.warn("Client error - gRPC Status: {}, Message: {}, Path: {}",
                    code, ex.getStatus().getDescription(), getPath(request));
        } else {
            log.error("Server error - gRPC Status: {}, Message: {}, Path: {}",
                    code, ex.getStatus().getDescription(), getPath(request), ex);
        }

        return ResponseEntity.status(httpStatus).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {

        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(java.time.LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Request validation failed")
                .path(getPath(request))
                .details(Map.of("validationErrors", validationErrors))
                .build();

        log.warn("Validation error - Path: {}, Errors: {}", getPath(request), validationErrors);

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleJsonParsingException(
            HttpMessageNotReadableException ex, WebRequest request) {

        String message = "Invalid JSON format";
        Map<String, Object> details = new HashMap<>();

        if (ex.getCause() instanceof InvalidFormatException invalidFormatEx) {
            message = String.format("Invalid value '%s' for field '%s'. Expected type: %s",
                    invalidFormatEx.getValue(),
                    invalidFormatEx.getPath().get(0).getFieldName(),
                    invalidFormatEx.getTargetType().getSimpleName());
            details.put("field", invalidFormatEx.getPath().get(0).getFieldName());
            details.put("rejectedValue", invalidFormatEx.getValue());
            details.put("expectedType", invalidFormatEx.getTargetType().getSimpleName());
        } else if (ex.getCause() instanceof MismatchedInputException mismatchedEx) {
            message = String.format("Invalid input for field '%s'",
                    mismatchedEx.getPath().isEmpty() ? "unknown"
                    : mismatchedEx.getPath().get(0).getFieldName());
        } else if (ex.getCause() instanceof JsonMappingException jsonEx) {
            message = "JSON mapping error: " + jsonEx.getOriginalMessage();
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(java.time.LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("JSON Parsing Error")
                .message(message)
                .path(getPath(request))
                .details(details.isEmpty() ? null : details)
                .build();

        log.warn("JSON parsing error - Path: {}, Message: {}", getPath(request), message);

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex, WebRequest request) {

        String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s",
                ex.getValue(), ex.getName(), ex.getRequiredType().getSimpleName());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(java.time.LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Type Mismatch")
                .message(message)
                .path(getPath(request))
                .details(Map.of(
                        "parameter", ex.getName(),
                        "rejectedValue", ex.getValue(),
                        "expectedType", ex.getRequiredType().getSimpleName()
                ))
                .build();

        log.warn("Type mismatch error - Path: {}, Parameter: {}, Value: {}",
                getPath(request), ex.getName(), ex.getValue());

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<ErrorResponse> handleDateTimeParseException(
            DateTimeParseException ex, WebRequest request) {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(java.time.LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Date Parse Error")
                .message("Invalid date/time format: " + ex.getParsedString())
                .path(getPath(request))
                .details(Map.of("rejectedValue", ex.getParsedString()))
                .build();

        log.warn("Date parsing error - Path: {}, Value: {}", getPath(request), ex.getParsedString());

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {

        ErrorResponse errorResponse = ErrorResponse.from(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage()
        );
        errorResponse.setPath(getPath(request));

        log.warn("Illegal argument - Path: {}, Message: {}", getPath(request), ex.getMessage());

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameterException(
            MissingServletRequestParameterException ex, WebRequest request) {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(java.time.LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Missing Parameter")
                .message(String.format("Required parameter '%s' is missing", ex.getParameterName()))
                .path(getPath(request))
                .details(Map.of("parameter", ex.getParameterName()))
                .build();

        log.warn("Missing parameter - Path: {}, Parameter: {}", getPath(request), ex.getParameterName());

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(MissingPathVariableException.class)
    public ResponseEntity<ErrorResponse> handleMissingPathVariableException(
            MissingPathVariableException ex, WebRequest request) {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(java.time.LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Missing Path Variable")
                .message(String.format("Required path variable '%s' is missing", ex.getVariableName()))
                .path(getPath(request))
                .details(Map.of("pathVariable", ex.getVariableName()))
                .build();

        log.warn("Missing path variable - Path: {}, Variable: {}", getPath(request), ex.getVariableName());

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {

        ErrorResponse errorResponse = ErrorResponse.from(
                HttpStatus.FORBIDDEN.value(),
                "Access Denied",
                "You don't have permission to access this resource"
        );
        errorResponse.setPath(getPath(request));

        log.warn("Access denied - Path: {}, Message: {}", getPath(request), ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex, WebRequest request) {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(java.time.LocalDateTime.now())
                .status(HttpStatus.METHOD_NOT_ALLOWED.value())
                .error("Method Not Allowed")
                .message(String.format("HTTP method '%s' is not supported for this endpoint", ex.getMethod()))
                .path(getPath(request))
                .details(Map.of(
                        "method", ex.getMethod(),
                        "supportedMethods", ex.getSupportedMethods()
                ))
                .build();

        log.warn("Method not allowed - Path: {}, Method: {}", getPath(request), ex.getMethod());

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorResponse);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(
            NoHandlerFoundException ex, WebRequest request) {

        ErrorResponse errorResponse = ErrorResponse.from(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                "The requested endpoint was not found"
        );
        errorResponse.setPath(getPath(request));

        log.warn("No handler found - Path: {}", getPath(request));

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException ex, WebRequest request) {

        if (ex.getMessage() != null && ex.getMessage().contains("service is currently unavailable")) {
            ErrorResponse errorResponse = ErrorResponse.from(
                    HttpStatus.SERVICE_UNAVAILABLE.value(),
                    "Service Unavailable",
                    ex.getMessage()
            );
            errorResponse.setPath(getPath(request));

            log.warn("Service unavailable - Path: {}, Message: {}", getPath(request), ex.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
        }

        ErrorResponse errorResponse = ErrorResponse.from(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected error occurred"
        );
        errorResponse.setPath(getPath(request));

        log.error("Unexpected runtime error - Path: {}", getPath(request), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {

        ErrorResponse errorResponse = ErrorResponse.from(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected error occurred"
        );
        errorResponse.setPath(getPath(request));

        log.error("Unexpected error - Path: {}", getPath(request), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    private HttpStatus mapGrpcStatusToHttp(Status.Code grpcCode) {
        return switch (grpcCode) {
            case OK ->
                HttpStatus.OK;
            case CANCELLED ->
                HttpStatus.REQUEST_TIMEOUT;
            case UNKNOWN ->
                HttpStatus.INTERNAL_SERVER_ERROR;
            case INVALID_ARGUMENT ->
                HttpStatus.BAD_REQUEST;
            case DEADLINE_EXCEEDED ->
                HttpStatus.REQUEST_TIMEOUT;
            case NOT_FOUND ->
                HttpStatus.NOT_FOUND;
            case ALREADY_EXISTS ->
                HttpStatus.CONFLICT;
            case PERMISSION_DENIED ->
                HttpStatus.FORBIDDEN;
            case RESOURCE_EXHAUSTED ->
                HttpStatus.TOO_MANY_REQUESTS;
            case FAILED_PRECONDITION ->
                HttpStatus.BAD_REQUEST;
            case ABORTED ->
                HttpStatus.CONFLICT;
            case OUT_OF_RANGE ->
                HttpStatus.BAD_REQUEST;
            case UNIMPLEMENTED ->
                HttpStatus.NOT_IMPLEMENTED;
            case INTERNAL ->
                HttpStatus.INTERNAL_SERVER_ERROR;
            case UNAVAILABLE ->
                HttpStatus.SERVICE_UNAVAILABLE;
            case DATA_LOSS ->
                HttpStatus.INTERNAL_SERVER_ERROR;
            case UNAUTHENTICATED ->
                HttpStatus.UNAUTHORIZED;
            default ->
                HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private boolean isClientError(Status.Code code) {
        return switch (code) {
            case INVALID_ARGUMENT, NOT_FOUND, ALREADY_EXISTS, PERMISSION_DENIED, FAILED_PRECONDITION, OUT_OF_RANGE, UNAUTHENTICATED ->
                true;
            default ->
                false;
        };
    }

    private String getPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
