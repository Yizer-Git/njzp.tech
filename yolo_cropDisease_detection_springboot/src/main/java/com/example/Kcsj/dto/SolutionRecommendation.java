package com.example.Kcsj.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Value object returned to the frontend with plan details and advisories.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SolutionRecommendation implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long diseaseId;
    private String diseaseName;
    private String diseaseRiskLevel;

    private Long cropId;
    private String cropName;

    private Long remedyId;
    private String remedyName;
    private String activeIngredient;
    private String formulation;
    private Integer intervalDays;
    private Integer safetyIntervalDays;
    private Integer maxApplicationsPerSeason;

    private BigDecimal recommendedDosage;
    private String dosageUnit;
    private BigDecimal computedCost;
    private String currency;
    private String costBreakdown;

    private String applicationStage;
    private String applicationTiming;
    private List<String> applicationNotes;
    private List<String> riskWarnings;

    private String weatherAdvisory;
    private WeatherData weatherData;

    /**
     * 推荐的施药时间窗口列表（如："今日 16:00-18:00"、"明日 08:00-10:00"）
     */
    private List<String> recommendedTimeWindows;
    
    /**
     * 各气象因素的限制详情（key: 因素名称, value: 限制说明）
     */
    private Map<String, String> applicationRestrictions;

    private LocalDateTime generatedAt;
}
