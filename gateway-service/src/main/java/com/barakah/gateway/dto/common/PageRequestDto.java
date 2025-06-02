package com.barakah.gateway.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Min;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageRequestDto {
    
    @Min(value = 0, message = "Page number must be non-negative")
    private Integer page = 0;
    
    @Min(value = 1, message = "Page size must be positive")
    private Integer size = 20;
    
    private String sort;
    private String direction = "ASC";
}