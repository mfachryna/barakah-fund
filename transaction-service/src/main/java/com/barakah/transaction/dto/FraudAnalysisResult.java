package com.barakah.transaction.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class FraudAnalysisResult {
    private BigDecimal fraudScore;
    private boolean isHighRisk;
    private List<String> reasons;
    private boolean aiAnalysis;
}