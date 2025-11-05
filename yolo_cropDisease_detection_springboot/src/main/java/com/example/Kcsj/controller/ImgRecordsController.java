package com.example.Kcsj.controller;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.Kcsj.common.Result;
import com.example.Kcsj.dto.DiseaseMappingResult;
import com.example.Kcsj.entity.ImgRecords;
import com.example.Kcsj.mapper.ImgRecordsMapper;
import com.example.Kcsj.service.DiseaseMapperService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/imgRecords")
public class ImgRecordsController {
    @Resource
    ImgRecordsMapper imgRecordsMapper;

    @Resource
    DiseaseMapperService diseaseMapperService;

    @GetMapping("/all")
    public Result<?> GetAll() {
        return Result.success(imgRecordsMapper.selectList(null));
    }
    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable int id) {
        System.out.println(id);
        return Result.success(imgRecordsMapper.selectById(id));
    }

    @GetMapping
    public Result<?> findPage(@RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize,
                              @RequestParam(defaultValue = "") String search,
                              @RequestParam(defaultValue = "") String search1,
                              @RequestParam(defaultValue = "") String search3,
                              @RequestParam(defaultValue = "") String search2,
                              @RequestParam(required = false) Integer taskId) {
        LambdaQueryWrapper<ImgRecords> wrapper = Wrappers.<ImgRecords>lambdaQuery();
        wrapper.orderByDesc(ImgRecords::getStartTime);
        if (StrUtil.isNotBlank(search)) {
            wrapper.like(ImgRecords::getUsername, search);
        }
        if (StrUtil.isNotBlank(search1)) {
            wrapper.like(ImgRecords::getKind, search1);
        }
        if (StrUtil.isNotBlank(search2)) {
            wrapper.like(ImgRecords::getLable, search2);
        }
        if (StrUtil.isNotBlank(search3)) {
            wrapper.like(ImgRecords::getConf, search3);
        }
        if (taskId != null) {
            wrapper.eq(ImgRecords::getTaskId, taskId);
        }
        Page<ImgRecords> Page = imgRecordsMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return Result.success(Page);
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable int id) {
        imgRecordsMapper.deleteById(id);
        return Result.success();
    }

    @PostMapping("/update")
    public Result<?> updates(@RequestBody ImgRecords imgrecords) {
        imgRecordsMapper.updateById(imgrecords);
        return Result.success();
    }


    @PostMapping
    public Result<?> save(@RequestBody ImgRecords imgrecords) {
        System.out.println(imgrecords);
        imgRecordsMapper.insert(imgrecords);
        return Result.success();
    }

    /**
     * Get disease mapping for a recognition record.
     * Maps YOLO labels to disease knowledge base entries.
     *
     * @param id record ID
     * @return disease mapping result(s)
     */
    @GetMapping("/{id}/disease-mapping")
    public Result<List<DiseaseMappingResult>> getDiseaseMapping(@PathVariable Integer id) {
        ImgRecords record = imgRecordsMapper.selectById(id);
        if (record == null) {
            return Result.error("404", "识别记录不存在");
        }

        String label = record.getLable();
        String cropType = record.getKind();

        if (StrUtil.isBlank(label) || StrUtil.isBlank(cropType)) {
            return Result.error("400", "识别记录缺少必要信息");
        }

        List<DiseaseMappingResult> results = new ArrayList<>();

        try {
            // Parse label - it might be a JSON array of multiple labels
            List<String> labels = new ArrayList<>();
            if (label.startsWith("[")) {
                // JSON array format
                JSONArray jsonArray = JSON.parseArray(label);
                for (int i = 0; i < jsonArray.size(); i++) {
                    labels.add(jsonArray.getString(i));
                }
            } else {
                // Single label
                labels.add(label);
            }

            // Map each label to disease
            for (String singleLabel : labels) {
                try {
                    DiseaseMappingResult mapping = diseaseMapperService.mapLabelToDisease(singleLabel, cropType);
                    if (mapping != null) {
                        results.add(mapping);
                        // For most cases, we only need the first valid mapping
                        if (!mapping.getIsHealthy()) {
                            break;
                        }
                    }
                } catch (IllegalArgumentException e) {
                    // Log but continue with next label
                    System.err.println("Failed to map label: " + singleLabel + ", error: " + e.getMessage());
                }
            }

            if (results.isEmpty()) {
                return Result.error("404", "未找到匹配的病害信息");
            }

            return Result.success(results);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("500", "病害映射失败: " + e.getMessage());
        }
    }
}
