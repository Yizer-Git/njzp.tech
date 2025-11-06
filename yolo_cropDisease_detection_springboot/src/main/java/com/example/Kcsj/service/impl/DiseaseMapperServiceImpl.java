package com.example.Kcsj.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.Kcsj.dto.DiseaseMappingResult;
import com.example.Kcsj.entity.DiseaseInfo;
import com.example.Kcsj.mapper.DiseaseInfoMapper;
import com.example.Kcsj.service.DiseaseMapperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of disease label mapping service with rule-based mapping strategy.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DiseaseMapperServiceImpl implements DiseaseMapperService {

    private final DiseaseInfoMapper diseaseInfoMapper;

    // Mapping rules: YOLO label pattern -> disease_code pattern
    private static final Map<String, String> LABEL_TO_CODE_RULES = new HashMap<>();

    static {
        // Corn / Maize diseases
        LABEL_TO_CODE_RULES.put("large_spot", "LARGE_SPOT");
        LABEL_TO_CODE_RULES.put("large spot", "LARGE_SPOT");
        LABEL_TO_CODE_RULES.put("viral_disease", "VIRAL_DISEASE");
        LABEL_TO_CODE_RULES.put("viral disease", "VIRAL_DISEASE");
        LABEL_TO_CODE_RULES.put("virus_disease", "VIRAL_DISEASE");
        LABEL_TO_CODE_RULES.put("virus disease", "VIRAL_DISEASE");
        LABEL_TO_CODE_RULES.put("rust", "RUST");
        LABEL_TO_CODE_RULES.put("small_spot", "SMALL_SPOT");
        LABEL_TO_CODE_RULES.put("small spot", "SMALL_SPOT");
        LABEL_TO_CODE_RULES.put("small-lesion", "SMALL_SPOT");

        // Rice diseases
        LABEL_TO_CODE_RULES.put("rice_blast", "RICE_BLAST");
        LABEL_TO_CODE_RULES.put("rice blast", "RICE_BLAST");
        LABEL_TO_CODE_RULES.put("sheath_blight", "SHEATH_BLIGHT");
        LABEL_TO_CODE_RULES.put("sheath blight", "SHEATH_BLIGHT");
        LABEL_TO_CODE_RULES.put("bacterial_blight", "BACTERIAL_BLIGHT");
        LABEL_TO_CODE_RULES.put("bacterial blight", "BACTERIAL_BLIGHT");

        // Tomato diseases
        LABEL_TO_CODE_RULES.put("late_blight", "LATE_BLIGHT");
        LABEL_TO_CODE_RULES.put("late blight", "LATE_BLIGHT");
        LABEL_TO_CODE_RULES.put("late-blight", "LATE_BLIGHT");
        LABEL_TO_CODE_RULES.put("gray_mold", "GRAY_MOLD");
        LABEL_TO_CODE_RULES.put("gray mold", "GRAY_MOLD");
        LABEL_TO_CODE_RULES.put("grey_mold", "GRAY_MOLD");
        LABEL_TO_CODE_RULES.put("grey mold", "GRAY_MOLD");
        LABEL_TO_CODE_RULES.put("downy_mildew", "DOWNY_MILDEW");
        LABEL_TO_CODE_RULES.put("downy mildew", "DOWNY_MILDEW");
        LABEL_TO_CODE_RULES.put("early_blight", "EARLY_BLIGHT");
        LABEL_TO_CODE_RULES.put("early blight", "EARLY_BLIGHT");
        LABEL_TO_CODE_RULES.put("leaf_miner", "LEAF_MINER");
        LABEL_TO_CODE_RULES.put("leaf miner", "LEAF_MINER");
        LABEL_TO_CODE_RULES.put("leafminer", "LEAF_MINER");
        LABEL_TO_CODE_RULES.put("aphids", "APHIDS");
        LABEL_TO_CODE_RULES.put("aphid", "APHIDS");
        LABEL_TO_CODE_RULES.put("leaf_mold", "LEAF_MOLD");
        LABEL_TO_CODE_RULES.put("leaf mold", "LEAF_MOLD");
        LABEL_TO_CODE_RULES.put("bacterial_speck", "BACTERIAL_SPECK");
        LABEL_TO_CODE_RULES.put("bacterial speck", "BACTERIAL_SPECK");
        LABEL_TO_CODE_RULES.put("canker", "CANKER");

        // Strawberry diseases
        LABEL_TO_CODE_RULES.put("angular_leaf_spot", "ANGULAR_LEAF_SPOT");
        LABEL_TO_CODE_RULES.put("angular leaf spot", "ANGULAR_LEAF_SPOT");
        LABEL_TO_CODE_RULES.put("angular_leafspot", "ANGULAR_LEAF_SPOT");
        LABEL_TO_CODE_RULES.put("angular leafspot", "ANGULAR_LEAF_SPOT");
        LABEL_TO_CODE_RULES.put("powdery_mildew", "POWDERY_MILDEW");
        LABEL_TO_CODE_RULES.put("powdery mildew", "POWDERY_MILDEW");
        LABEL_TO_CODE_RULES.put("powdery-mildew", "POWDERY_MILDEW");
        LABEL_TO_CODE_RULES.put("anthracnose_fruit_rot", "ANTHRACNOSE_FRUIT_ROT");
        LABEL_TO_CODE_RULES.put("anthracnose fruit rot", "ANTHRACNOSE_FRUIT_ROT");
        LABEL_TO_CODE_RULES.put("anthracnose", "ANTHRACNOSE_FRUIT_ROT");
        LABEL_TO_CODE_RULES.put("blossom_blight", "BLOSSOM_BLIGHT");
        LABEL_TO_CODE_RULES.put("blossom blight", "BLOSSOM_BLIGHT");
        LABEL_TO_CODE_RULES.put("leaf_spot", "LEAF_SPOT");
        LABEL_TO_CODE_RULES.put("leaf spot", "LEAF_SPOT");
        LABEL_TO_CODE_RULES.put("black_root_rot", "BLACK_ROOT_ROT");
        LABEL_TO_CODE_RULES.put("black root rot", "BLACK_ROOT_ROT");
        LABEL_TO_CODE_RULES.put("black-root-rot", "BLACK_ROOT_ROT");

        // Citrus diseases
        LABEL_TO_CODE_RULES.put("huanglongbing", "HUANGLONGBING");
        LABEL_TO_CODE_RULES.put("huang long bing", "HUANGLONGBING");
        LABEL_TO_CODE_RULES.put("citrus_canker", "CITRUS_CANKER");
        LABEL_TO_CODE_RULES.put("citrus canker", "CITRUS_CANKER");
        LABEL_TO_CODE_RULES.put("citrus_anthracnose", "CITRUS_ANTHRACNOSE");
        LABEL_TO_CODE_RULES.put("citrus anthracnose", "CITRUS_ANTHRACNOSE");
        LABEL_TO_CODE_RULES.put("resin_disease", "RESIN_DISEASE");
        LABEL_TO_CODE_RULES.put("resin disease", "RESIN_DISEASE");
        LABEL_TO_CODE_RULES.put("greasy_spot", "GREASY_SPOT");
        LABEL_TO_CODE_RULES.put("greasy spot", "GREASY_SPOT");
    }

    private static final Set<String> HEALTHY_KEYWORDS = new HashSet<>(Arrays.asList(
            "health", "healthy", "normal", "健康"
    ));

    private static final Map<String, String> CROP_TYPE_TO_PREFIX = new HashMap<>();

    static {
        CROP_TYPE_TO_PREFIX.put("corn", "CORN");
        CROP_TYPE_TO_PREFIX.put("maize", "CORN");
        CROP_TYPE_TO_PREFIX.put("rice", "RICE");
        CROP_TYPE_TO_PREFIX.put("wheat", "WHEAT");
        CROP_TYPE_TO_PREFIX.put("tomato", "TOMATO");
        CROP_TYPE_TO_PREFIX.put("strawberry", "STRAWBERRY");
        CROP_TYPE_TO_PREFIX.put("citrus", "CITRUS");
    }

    @Override
    public DiseaseMappingResult mapLabelToDisease(String label, String cropType) {
        if (StrUtil.isBlank(label)) {
            throw new IllegalArgumentException("Label不能为空");
        }
        if (StrUtil.isBlank(cropType)) {
            throw new IllegalArgumentException("作物类型不能为空");
        }

        // Normalize inputs
        String normalizedLabel = normalizeLabel(label);
        String normalizedCropType = cropType.trim().toLowerCase();

        // Check if healthy
        if (isHealthyLabel(normalizedLabel)) {
            return DiseaseMappingResult.builder()
                    .originalLabel(label)
                    .cropName(capitalizeFirst(normalizedCropType))
                    .isHealthy(true)
                    .build();
        }

        // Extract disease key from label
        String diseaseKey = extractDiseaseKey(normalizedLabel);
        if (diseaseKey == null) {
            throw new IllegalArgumentException("无法从标签中提取病害信息: " + label);
        }

        // Build disease_code
        String cropPrefix = CROP_TYPE_TO_PREFIX.get(normalizedCropType);
        if (cropPrefix == null) {
            throw new IllegalArgumentException("不支持的作物类型: " + cropType);
        }

        // Try to find disease from database
        DiseaseInfo diseaseInfo = findDiseaseByLabelAndCrop(diseaseKey, cropPrefix, normalizedCropType);
        if (diseaseInfo == null) {
            log.warn("未找到匹配的病害信息: label={}, cropType={}, diseaseKey={}", label, cropType, diseaseKey);
            throw new IllegalArgumentException("未找到匹配的病害信息: " + label);
        }

        return DiseaseMappingResult.builder()
                .diseaseId(diseaseInfo.getId())
                .diseaseCode(diseaseInfo.getDiseaseCode())
                .diseaseName(diseaseInfo.getDiseaseName())
                .cropId(diseaseInfo.getCropId())
                .cropName(diseaseInfo.getCropName())
                .originalLabel(label)
                .riskLevel(diseaseInfo.getRiskLevel())
                .isHealthy(false)
                .build();
    }

    @Override
    public boolean isHealthyLabel(String label) {
        if (StrUtil.isBlank(label)) {
            return false;
        }
        String normalized = label.toLowerCase().trim();
        return HEALTHY_KEYWORDS.stream().anyMatch(normalized::contains);
    }

    private String normalizeLabel(String label) {
        // Remove special characters and normalize spacing
        String normalized = label.toLowerCase()
                .replaceAll("[（）()\\[\\]【】]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return normalized;
    }

    private String extractDiseaseKey(String normalizedLabel) {
        // Try to match against known patterns
        for (Map.Entry<String, String> entry : LABEL_TO_CODE_RULES.entrySet()) {
            if (normalizedLabel.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        // If no direct match, try to extract the English part before Chinese
        Pattern pattern = Pattern.compile("^([a-z_\\s]+?)\\s*[（(]");
        Matcher matcher = pattern.matcher(normalizedLabel);
        if (matcher.find()) {
            String extracted = matcher.group(1).trim().replaceAll("\\s+", "_");
            return extracted.toUpperCase();
        }

        // Extract any continuous English word/underscore sequence
        pattern = Pattern.compile("([a-z_]+)");
        matcher = pattern.matcher(normalizedLabel);
        if (matcher.find()) {
            return matcher.group(1).toUpperCase();
        }

        return null;
    }

    private DiseaseInfo findDiseaseByLabelAndCrop(String diseaseKey, String cropPrefix, String cropType) {
        // Strategy 1: Try exact match with crop prefix
        String exactCode = cropPrefix + "_" + diseaseKey;
        DiseaseInfo disease = diseaseInfoMapper.selectOne(
                new LambdaQueryWrapper<DiseaseInfo>()
                        .eq(DiseaseInfo::getDiseaseCode, exactCode)
                        .last("LIMIT 1")
        );
        if (disease != null) {
            return disease;
        }

        // Strategy 2: Try fuzzy match with disease_code LIKE pattern
        disease = diseaseInfoMapper.selectOne(
                new LambdaQueryWrapper<DiseaseInfo>()
                        .like(DiseaseInfo::getDiseaseCode, diseaseKey)
                        .eq(DiseaseInfo::getCropName, capitalizeFirst(cropType))
                        .last("LIMIT 1")
        );
        if (disease != null) {
            return disease;
        }

        // Strategy 3: Try match by disease_name
        disease = diseaseInfoMapper.selectOne(
                new LambdaQueryWrapper<DiseaseInfo>()
                        .like(DiseaseInfo::getDiseaseName, diseaseKey.replace("_", " "))
                        .eq(DiseaseInfo::getCropName, capitalizeFirst(cropType))
                        .last("LIMIT 1")
        );

        return disease;
    }

    @Override
    public Long mapFlaskLabelToDiseaseId(String label, String cropType) {
        try {
            DiseaseMappingResult result = mapLabelToDisease(label, cropType);
            return result.getDiseaseId();
        } catch (Exception e) {
            log.error("映射病害标签失败: label={}, cropType={}", label, cropType, e);
            return null;
        }
    }
    
    private String capitalizeFirst(String str) {
        if (StrUtil.isBlank(str)) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
