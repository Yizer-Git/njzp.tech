package com.example.Kcsj.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Lightweight catalog entry for selectable solution plans.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SolutionOption implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long solutionId;
    private Long diseaseId;
    private String diseaseName;
    private String diseaseRiskLevel;
    private Long cropId;
    private String cropName;
    private Long remedyId;
    private String remedyName;
    private BigDecimal recommendedDosage;
    private String dosageUnit;
}
