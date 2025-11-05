package com.example.Kcsj.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.Kcsj.dto.DiseaseEncyclopediaResponse;
import com.example.Kcsj.entity.DiseaseInfo;
import com.example.Kcsj.mapper.DiseaseInfoMapper;
import com.example.Kcsj.service.DiseaseEncyclopediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Default implementation for the disease encyclopedia aggregation service.
 */
@Service
@RequiredArgsConstructor
public class DiseaseEncyclopediaServiceImpl implements DiseaseEncyclopediaService {

    private static final Map<String, Integer> RISK_LEVEL_ORDER;
    private static final List<String> DEFAULT_RISK_LEVELS = Arrays.asList("HIGH", "MEDIUM", "LOW");
    private static final List<String> DEFAULT_CROP_ORDER = Arrays.asList("Corn", "Rice", "Tomato", "Strawberry", "Citrus");
    private static final Map<String, String> CROP_DISPLAY_NAME;

    static {
        Map<String, Integer> riskOrder = new LinkedHashMap<>();
        for (int i = 0; i < DEFAULT_RISK_LEVELS.size(); i++) {
            riskOrder.put(DEFAULT_RISK_LEVELS.get(i), i);
        }
        RISK_LEVEL_ORDER = Collections.unmodifiableMap(riskOrder);

        Map<String, String> cropDisplay = new LinkedHashMap<>();
        cropDisplay.put("Corn", "玉米");
        cropDisplay.put("Rice", "水稻");
        cropDisplay.put("Tomato", "番茄");
        cropDisplay.put("Strawberry", "草莓");
        cropDisplay.put("Citrus", "柑橘");
        CROP_DISPLAY_NAME = Collections.unmodifiableMap(cropDisplay);
    }

    private final DiseaseInfoMapper diseaseInfoMapper;

    @Override
    public DiseaseEncyclopediaResponse getEncyclopediaData() {
        List<DiseaseInfo> diseases = diseaseInfoMapper.selectList(null);
        if (diseases.isEmpty()) {
            return DiseaseEncyclopediaResponse.builder()
                    .overview(DiseaseEncyclopediaResponse.Overview.builder()
                            .totalDiseases(0)
                            .cropCount(0)
                            .highRiskCount(0)
                            .mediumRiskCount(0)
                            .lowRiskCount(0)
                            .build())
                    .crops(Collections.emptyList())
                    .pathogenTypes(Collections.emptyList())
                    .riskLevels(new ArrayList<>(DEFAULT_RISK_LEVELS))
                    .build();
        }

        Map<String, List<DiseaseInfo>> groupedByCrop = diseases.stream()
                .collect(Collectors.groupingBy(
                        disease -> StrUtil.blankToDefault(disease.getCropName(), "Unknown"),
                        Collectors.toList()
                ));

        List<String> orderedCropKeys = groupedByCrop.keySet().stream()
                .sorted(Comparator
                        .comparingInt((String crop) -> cropOrderKey(crop))
                        .thenComparing(crop -> crop.toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());

        List<DiseaseEncyclopediaResponse.CropDiseaseGroup> cropDiseaseGroups = orderedCropKeys.stream()
                .map(crop -> {
                    List<DiseaseInfo> cropDiseases = groupedByCrop.getOrDefault(crop, Collections.emptyList())
                            .stream()
                            .sorted(diseaseComparatorWithinCrop())
                            .collect(Collectors.toList());
                    return DiseaseEncyclopediaResponse.CropDiseaseGroup.builder()
                            .cropName(crop)
                            .cropDisplayName(resolveCropDisplayName(crop))
                            .diseaseCount(cropDiseases.size())
                            .diseases(cropDiseases)
                            .build();
                })
                .collect(Collectors.toList());

        DiseaseEncyclopediaResponse.Overview overview = buildOverview(diseases);
        List<String> pathogenTypes = extractPathogenTypes(diseases);
        List<String> riskLevels = extractRiskLevels(diseases);

        return DiseaseEncyclopediaResponse.builder()
                .overview(overview)
                .crops(cropDiseaseGroups)
                .pathogenTypes(pathogenTypes)
                .riskLevels(riskLevels)
                .build();
    }

    @Override
    public List<DiseaseInfo> search(String keyword) {
        String trimmed = StrUtil.trim(keyword);
        LambdaQueryWrapper<DiseaseInfo> wrapper = Wrappers.lambdaQuery();
        if (StrUtil.isNotBlank(trimmed)) {
            wrapper.like(DiseaseInfo::getDiseaseName, trimmed)
                    .or()
                    .like(DiseaseInfo::getDiseaseCode, trimmed)
                    .or()
                    .like(DiseaseInfo::getCropName, trimmed);
        }
        List<DiseaseInfo> matches = diseaseInfoMapper.selectList(wrapper);
        return matches.stream()
                .sorted(diseaseComparatorWithCrop())
                .collect(Collectors.toList());
    }

    private DiseaseEncyclopediaResponse.Overview buildOverview(List<DiseaseInfo> diseases) {
        long high = countByRisk(diseases, "HIGH");
        long medium = countByRisk(diseases, "MEDIUM");
        long low = countByRisk(diseases, "LOW");
        Set<String> cropNames = diseases.stream()
                .map(DiseaseInfo::getCropName)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
        return DiseaseEncyclopediaResponse.Overview.builder()
                .totalDiseases(diseases.size())
                .cropCount(cropNames.size())
                .highRiskCount(high)
                .mediumRiskCount(medium)
                .lowRiskCount(low)
                .build();
    }

    private long countByRisk(List<DiseaseInfo> diseases, String targetRisk) {
        return diseases.stream()
                .filter(disease -> targetRisk.equalsIgnoreCase(StrUtil.blankToDefault(disease.getRiskLevel(), "")))
                .count();
    }

    private List<String> extractPathogenTypes(List<DiseaseInfo> diseases) {
        return diseases.stream()
                .map(DiseaseInfo::getPathogenType)
                .filter(StrUtil::isNotBlank)
                .map(String::toUpperCase)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private List<String> extractRiskLevels(List<DiseaseInfo> diseases) {
        List<String> levels = diseases.stream()
                .map(DiseaseInfo::getRiskLevel)
                .filter(StrUtil::isNotBlank)
                .map(String::toUpperCase)
                .distinct()
                .sorted(Comparator.comparingInt(this::riskOrderKey))
                .collect(Collectors.toList());

        if (levels.isEmpty()) {
            return new ArrayList<>(DEFAULT_RISK_LEVELS);
        }

        // Ensure default ordering is preserved even if some levels are missing.
        List<String> ordered = new ArrayList<>();
        for (String level : DEFAULT_RISK_LEVELS) {
            if (levels.contains(level)) {
                ordered.add(level);
            }
        }
        // Append any additional custom levels (if any)
        for (String level : levels) {
            if (!ordered.contains(level)) {
                ordered.add(level);
            }
        }
        return ordered;
    }

    private Comparator<DiseaseInfo> diseaseComparatorWithinCrop() {
        return Comparator
                .comparingInt((DiseaseInfo disease) -> riskOrderKey(disease.getRiskLevel()))
                .thenComparing(disease -> StrUtil.blankToDefault(disease.getDiseaseName(), "").toLowerCase(Locale.ROOT))
                .thenComparing(disease -> StrUtil.blankToDefault(disease.getDiseaseCode(), "").toLowerCase(Locale.ROOT));
    }

    private Comparator<DiseaseInfo> diseaseComparatorWithCrop() {
        return Comparator
                .comparingInt((DiseaseInfo disease) -> cropOrderKey(StrUtil.blankToDefault(disease.getCropName(), "Unknown")))
                .thenComparing(disease -> StrUtil.blankToDefault(disease.getCropName(), "").toLowerCase(Locale.ROOT))
                .thenComparingInt(disease -> riskOrderKey(disease.getRiskLevel()))
                .thenComparing(disease -> StrUtil.blankToDefault(disease.getDiseaseName(), "").toLowerCase(Locale.ROOT))
                .thenComparing(disease -> StrUtil.blankToDefault(disease.getDiseaseCode(), "").toLowerCase(Locale.ROOT));
    }

    private int cropOrderKey(String crop) {
        String normalized = StrUtil.blankToDefault(crop, "Unknown");
        int index = DEFAULT_CROP_ORDER.indexOf(normalized);
        return index >= 0 ? index : DEFAULT_CROP_ORDER.size();
    }

    private int riskOrderKey(String riskLevel) {
        String normalized = StrUtil.blankToDefault(riskLevel, "").toUpperCase();
        return RISK_LEVEL_ORDER.getOrDefault(normalized, DEFAULT_RISK_LEVELS.size());
    }

    private String resolveCropDisplayName(String crop) {
        String normalized = StrUtil.blankToDefault(crop, "Unknown");
        return CROP_DISPLAY_NAME.getOrDefault(normalized, normalized);
    }
}
