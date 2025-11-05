package com.example.Kcsj.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.Kcsj.common.Result;
import com.example.Kcsj.entity.SolutionPlan;
import com.example.Kcsj.mapper.SolutionPlanMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Solution plan management controller
 */
@RestController
@RequestMapping("/solution-plan")
@RequiredArgsConstructor
public class SolutionPlanController {

    private final SolutionPlanMapper solutionPlanMapper;

    /**
     * Get all solution plans
     */
    @GetMapping("/all")
    public Result<?> getAllPlans() {
        return Result.success(solutionPlanMapper.selectList(null));
    }

    /**
     * Get solution plan by ID
     */
    @GetMapping("/{id}")
    public Result<?> getPlanById(@PathVariable Long id) {
        SolutionPlan plan = solutionPlanMapper.selectById(id);
        if (plan == null) {
            return Result.error("404", "方案信息不存在");
        }
        return Result.success(plan);
    }

    /**
     * Get solution plans with pagination and filters
     */
    @GetMapping("/list")
    public Result<?> getPlanList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long diseaseId,
            @RequestParam(required = false) Long cropId,
            @RequestParam(defaultValue = "") String cropName,
            @RequestParam(defaultValue = "") String status) {
        
        LambdaQueryWrapper<SolutionPlan> wrapper = Wrappers.<SolutionPlan>lambdaQuery();
        
        if (diseaseId != null) {
            wrapper.eq(SolutionPlan::getDiseaseId, diseaseId);
        }
        
        if (cropId != null) {
            wrapper.eq(SolutionPlan::getCropId, cropId);
        }
        
        if (StrUtil.isNotBlank(cropName)) {
            wrapper.like(SolutionPlan::getCropName, cropName);
        }
        
        if (StrUtil.isNotBlank(status)) {
            wrapper.eq(SolutionPlan::getStatus, status);
        }
        
        wrapper.orderByDesc(SolutionPlan::getUpdatedAt);
        
        Page<SolutionPlan> page = solutionPlanMapper.selectPage(
            new Page<>(pageNum, pageSize), 
            wrapper
        );
        
        return Result.success(page);
    }

    /**
     * Create new solution plan
     */
    @PostMapping
    public Result<?> createPlan(@RequestBody SolutionPlan plan) {
        if (plan.getDiseaseId() == null || plan.getRemedyId() == null) {
            return Result.error("400", "病害ID和药剂ID不能为空");
        }
        
        // Set default status
        if (StrUtil.isBlank(plan.getStatus())) {
            plan.setStatus("ACTIVE");
        }
        
        solutionPlanMapper.insert(plan);
        return Result.success(plan);
    }

    /**
     * Update solution plan
     */
    @PutMapping("/{id}")
    public Result<?> updatePlan(@PathVariable Long id, @RequestBody SolutionPlan plan) {
        SolutionPlan existing = solutionPlanMapper.selectById(id);
        if (existing == null) {
            return Result.error("404", "方案信息不存在");
        }
        
        plan.setId(id);
        solutionPlanMapper.updateById(plan);
        return Result.success(plan);
    }

    /**
     * Update solution plan status
     */
    @PutMapping("/{id}/status")
    public Result<?> updateStatus(
            @PathVariable Long id, 
            @RequestParam String status) {
        
        SolutionPlan plan = solutionPlanMapper.selectById(id);
        if (plan == null) {
            return Result.error("404", "方案信息不存在");
        }
        
        if (!status.equals("ACTIVE") && !status.equals("INACTIVE") && !status.equals("ARCHIVED")) {
            return Result.error("400", "无效的状态值");
        }
        
        plan.setStatus(status);
        solutionPlanMapper.updateById(plan);
        return Result.success(plan);
    }

    /**
     * Delete solution plan
     */
    @DeleteMapping("/{id}")
    public Result<?> deletePlan(@PathVariable Long id) {
        SolutionPlan plan = solutionPlanMapper.selectById(id);
        if (plan == null) {
            return Result.error("404", "方案信息不存在");
        }
        
        solutionPlanMapper.deleteById(id);
        return Result.success();
    }
}

