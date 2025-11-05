package com.example.Kcsj.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.Kcsj.common.Result;
import com.example.Kcsj.entity.DiseaseInfo;
import com.example.Kcsj.mapper.DiseaseInfoMapper;
import com.example.Kcsj.service.DiseaseEncyclopediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Disease information management controller
 */
@RestController
@RequestMapping("/disease")
@RequiredArgsConstructor
public class DiseaseInfoController {

    private final DiseaseInfoMapper diseaseInfoMapper;
    private final DiseaseEncyclopediaService diseaseEncyclopediaService;

    /**
     * Get aggregated encyclopedia data (grouped by crop).
     */
    @GetMapping("/encyclopedia")
    public Result<?> getEncyclopediaData() {
        return Result.success(diseaseEncyclopediaService.getEncyclopediaData());
    }

    /**
     * Search diseases for encyclopedia quick lookup.
     */
    @GetMapping("/encyclopedia/search")
    public Result<?> searchEncyclopedia(@RequestParam(defaultValue = "") String keyword) {
        return Result.success(diseaseEncyclopediaService.search(keyword));
    }

    /**
     * Get all diseases
     */
    @GetMapping("/all")
    public Result<?> getAllDiseases() {
        return Result.success(diseaseInfoMapper.selectList(null));
    }

    /**
     * Get disease by ID
     */
    @GetMapping("/{id}")
    public Result<?> getDiseaseById(@PathVariable Long id) {
        DiseaseInfo disease = diseaseInfoMapper.selectById(id);
        if (disease == null) {
            return Result.error("404", "病害信息不存在");
        }
        return Result.success(disease);
    }

    /**
     * Get diseases with pagination and filters
     */
    @GetMapping("/list")
    public Result<?> getDiseaseList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(defaultValue = "") String diseaseName,
            @RequestParam(defaultValue = "") String cropName,
            @RequestParam(defaultValue = "") String riskLevel) {
        
        LambdaQueryWrapper<DiseaseInfo> wrapper = Wrappers.<DiseaseInfo>lambdaQuery();
        
        if (StrUtil.isNotBlank(diseaseName)) {
            wrapper.like(DiseaseInfo::getDiseaseName, diseaseName)
                   .or()
                   .like(DiseaseInfo::getDiseaseCode, diseaseName);
        }
        
        if (StrUtil.isNotBlank(cropName)) {
            wrapper.like(DiseaseInfo::getCropName, cropName);
        }
        
        if (StrUtil.isNotBlank(riskLevel)) {
            wrapper.eq(DiseaseInfo::getRiskLevel, riskLevel);
        }
        
        wrapper.orderByDesc(DiseaseInfo::getUpdatedAt);
        
        Page<DiseaseInfo> page = diseaseInfoMapper.selectPage(
            new Page<>(pageNum, pageSize), 
            wrapper
        );
        
        return Result.success(page);
    }

    /**
     * Create new disease
     */
    @PostMapping
    public Result<?> createDisease(@RequestBody DiseaseInfo disease) {
        if (StrUtil.isBlank(disease.getDiseaseCode()) || StrUtil.isBlank(disease.getDiseaseName())) {
            return Result.error("400", "病害代码和名称不能为空");
        }
        
        // Check if disease code already exists
        Integer count = diseaseInfoMapper.selectCount(
            Wrappers.<DiseaseInfo>lambdaQuery()
                .eq(DiseaseInfo::getDiseaseCode, disease.getDiseaseCode())
        );
        
        if (count > 0) {
            return Result.error("400", "病害代码已存在");
        }
        
        diseaseInfoMapper.insert(disease);
        return Result.success(disease);
    }

    /**
     * Update disease
     */
    @PutMapping("/{id}")
    public Result<?> updateDisease(@PathVariable Long id, @RequestBody DiseaseInfo disease) {
        DiseaseInfo existing = diseaseInfoMapper.selectById(id);
        if (existing == null) {
            return Result.error("404", "病害信息不存在");
        }
        
        disease.setId(id);
        diseaseInfoMapper.updateById(disease);
        return Result.success(disease);
    }

    /**
     * Delete disease
     */
    @DeleteMapping("/{id}")
    public Result<?> deleteDisease(@PathVariable Long id) {
        DiseaseInfo disease = diseaseInfoMapper.selectById(id);
        if (disease == null) {
            return Result.error("404", "病害信息不存在");
        }
        
        diseaseInfoMapper.deleteById(id);
        return Result.success();
    }
}

