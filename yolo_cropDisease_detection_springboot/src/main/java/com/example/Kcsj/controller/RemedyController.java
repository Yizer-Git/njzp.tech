package com.example.Kcsj.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.Kcsj.common.Result;
import com.example.Kcsj.entity.Remedy;
import com.example.Kcsj.mapper.RemedyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Remedy (pesticide/fungicide) management controller
 */
@RestController
@RequestMapping("/remedy")
@RequiredArgsConstructor
public class RemedyController {

    private final RemedyMapper remedyMapper;

    /**
     * Get all remedies
     */
    @GetMapping("/all")
    public Result<?> getAllRemedies() {
        return Result.success(remedyMapper.selectList(null));
    }

    /**
     * Get remedy by ID
     */
    @GetMapping("/{id}")
    public Result<?> getRemedyById(@PathVariable Long id) {
        Remedy remedy = remedyMapper.selectById(id);
        if (remedy == null) {
            return Result.error("404", "药剂信息不存在");
        }
        return Result.success(remedy);
    }

    /**
     * Get remedies with pagination and filters
     */
    @GetMapping("/list")
    public Result<?> getRemedyList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(defaultValue = "") String remedyName,
            @RequestParam(defaultValue = "") String activeIngredient,
            @RequestParam(defaultValue = "") String targetPathogen) {
        
        LambdaQueryWrapper<Remedy> wrapper = Wrappers.<Remedy>lambdaQuery();
        
        if (StrUtil.isNotBlank(remedyName)) {
            wrapper.like(Remedy::getRemedyName, remedyName)
                   .or()
                   .like(Remedy::getRemedyCode, remedyName);
        }
        
        if (StrUtil.isNotBlank(activeIngredient)) {
            wrapper.like(Remedy::getActiveIngredient, activeIngredient);
        }
        
        if (StrUtil.isNotBlank(targetPathogen)) {
            wrapper.like(Remedy::getTargetPathogen, targetPathogen);
        }
        
        wrapper.orderByDesc(Remedy::getUpdatedAt);
        
        Page<Remedy> page = remedyMapper.selectPage(
            new Page<>(pageNum, pageSize), 
            wrapper
        );
        
        return Result.success(page);
    }

    /**
     * Create new remedy
     */
    @PostMapping
    public Result<?> createRemedy(@RequestBody Remedy remedy) {
        if (StrUtil.isBlank(remedy.getRemedyCode()) || StrUtil.isBlank(remedy.getRemedyName())) {
            return Result.error("400", "药剂代码和名称不能为空");
        }
        
        // Check if remedy code already exists
        Integer count = remedyMapper.selectCount(
            Wrappers.<Remedy>lambdaQuery()
                .eq(Remedy::getRemedyCode, remedy.getRemedyCode())
        );
        
        if (count > 0) {
            return Result.error("400", "药剂代码已存在");
        }
        
        // Set price update timestamp
        remedy.setLastPriceUpdate(new Date());
        
        remedyMapper.insert(remedy);
        return Result.success(remedy);
    }

    /**
     * Update remedy
     */
    @PutMapping("/{id}")
    public Result<?> updateRemedy(@PathVariable Long id, @RequestBody Remedy remedy) {
        Remedy existing = remedyMapper.selectById(id);
        if (existing == null) {
            return Result.error("404", "药剂信息不存在");
        }
        
        remedy.setId(id);
        
        // If price changed, update timestamp
        if (remedy.getCostPerUnit() != null && 
            !remedy.getCostPerUnit().equals(existing.getCostPerUnit())) {
            remedy.setLastPriceUpdate(new Date());
        }
        
        remedyMapper.updateById(remedy);
        return Result.success(remedy);
    }

    /**
     * Update remedy price
     */
    @PutMapping("/{id}/price")
    public Result<?> updatePrice(
            @PathVariable Long id, 
            @RequestParam BigDecimal price) {
        
        Remedy remedy = remedyMapper.selectById(id);
        if (remedy == null) {
            return Result.error("404", "药剂信息不存在");
        }
        
        remedy.setCostPerUnit(price);
        remedy.setLastPriceUpdate(new Date());
        
        remedyMapper.updateById(remedy);
        return Result.success(remedy);
    }

    /**
     * Delete remedy
     */
    @DeleteMapping("/{id}")
    public Result<?> deleteRemedy(@PathVariable Long id) {
        Remedy remedy = remedyMapper.selectById(id);
        if (remedy == null) {
            return Result.error("404", "药剂信息不存在");
        }
        
        remedyMapper.deleteById(id);
        return Result.success();
    }
}

