package com.barakah.gateway.controller;

import com.barakah.gateway.dto.health.HealthResponseDto;
import com.barakah.gateway.dto.health.ServiceHealthDto;
import com.barakah.gateway.service.HealthCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
@Tag(name = "Health Check", description = "Service health monitoring")
public class HealthController {

    private final HealthCheckService healthCheckService;

    @GetMapping
    @Operation(summary = "Check overall health")
    public ResponseEntity<HealthResponseDto> checkHealth() {
        Map<String, ServiceHealthDto> serviceHealth = healthCheckService.checkAllServices();
        
        String overallStatus = serviceHealth.values().stream()
                .allMatch(health -> "UP".equals(health.getStatus())) ? "UP" : "DOWN";

        HealthResponseDto response = HealthResponseDto.builder()
                .status(overallStatus)
                .version("1.0.0")
                .timestamp(LocalDateTime.now())
                .services(serviceHealth)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/detailed")
    @Operation(summary = "Check detailed health")
    public ResponseEntity<Map<String, ServiceHealthDto>> checkDetailedHealth() {
        Map<String, ServiceHealthDto> serviceHealth = healthCheckService.checkAllServices();
        return ResponseEntity.ok(serviceHealth);
    }
}