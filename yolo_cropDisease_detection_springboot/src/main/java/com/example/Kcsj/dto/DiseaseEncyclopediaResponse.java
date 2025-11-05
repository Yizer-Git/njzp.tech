package com.example.Kcsj.dto;

import com.example.Kcsj.entity.DiseaseInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Aggregated payload for disease encyclopedia page.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DiseaseEncyclopediaResponse {

    private Overview overview;
    private List<CropDiseaseGroup> crops;
    private List<String> pathogenTypes;
    private List<String> riskLevels;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Overview {
        private int totalDiseases;
        private int cropCount;
        private long highRiskCount;
        private long mediumRiskCount;
        private long lowRiskCount;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CropDiseaseGroup {
        private String cropName;
        private String cropDisplayName;
        private int diseaseCount;
        private List<DiseaseInfo> diseases;
    }
}

