package com.barakah.gateway.dto.health;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceHealthDto {
    private String status;
    private String message;
    private Long responseTime;
    private LocalDateTime lastChecked;
}