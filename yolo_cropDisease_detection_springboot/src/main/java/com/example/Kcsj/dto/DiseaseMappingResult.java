package com.example.Kcsj.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Result of disease label mapping operation.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DiseaseMappingResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long diseaseId;
    private String diseaseCode;
    private String diseaseName;
    private Long cropId;
    private String cropName;
    private String originalLabel;
    private String riskLevel;
    private Boolean isHealthy;
}

