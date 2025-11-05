package com.example.Kcsj.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.Kcsj.common.Result;
import com.example.Kcsj.dto.DiseaseMappingResult;
import com.example.Kcsj.entity.VideoRecords;
import com.example.Kcsj.mapper.VideoRecordsMapper;
import com.example.Kcsj.service.DiseaseMapperService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/videoRecords")
public class VideoRecordsController {
    @Resource
    VideoRecordsMapper videoRecordsMapper;

    @Resource
    DiseaseMapperService diseaseMapperService;

    @GetMapping("/all")
    public Result<?> GetAll() {
        return Result.success(videoRecordsMapper.selectList(null));
    }
    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable int id) {
        System.out.println(id);
        return Result.success(videoRecordsMapper.selectById(id));
    }

    @GetMapping
    public Result<?> findPage(@RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize,
                              @RequestParam(defaultValue = "") String search,
                              @RequestParam(defaultValue = "") String search1,
                              @RequestParam(defaultValue = "") String search3,
                              @RequestParam(defaultValue = "") String search2,
                              @RequestParam(required = false) Integer taskId) {
        LambdaQueryWrapper<VideoRecords> wrapper = Wrappers.<VideoRecords>lambdaQuery();
        wrapper.orderByDesc(VideoRecords::getStartTime);
        if (StrUtil.isNotBlank(search)) {
            wrapper.like(VideoRecords::getUsername, search);
        }
        if (StrUtil.isNotBlank(search1)) {
            wrapper.like(VideoRecords::getKind, search1);
        }
        if (StrUtil.isNotBlank(search2)) {
            wrapper.like(VideoRecords::getWeight, search2);
        }
        if (StrUtil.isNotBlank(search3)) {
            wrapper.like(VideoRecords::getConf, search3);
        }
        if (taskId != null) {
            wrapper.eq(VideoRecords::getTaskId, taskId);
        }
        Page<VideoRecords> Page = videoRecordsMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return Result.success(Page);
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable int id) {
        videoRecordsMapper.deleteById(id);
        return Result.success();
    }

    @PostMapping("/update")
    public Result<?> updates(@RequestBody VideoRecords videoRecords) {
        videoRecordsMapper.updateById(videoRecords);
        return Result.success();
    }


    @PostMapping
    public Result<?> save(@RequestBody VideoRecords videoRecords) {
        System.out.println(videoRecords);
        videoRecordsMapper.insert(videoRecords);
        return Result.success();
    }

    /**
     * Get disease mapping for a video record.
     * Since video records don't store labels, label must be provided as query parameter.
     *
     * @param id    record ID
     * @param label detected disease label from video analysis
     * @return disease mapping result
     */
    @GetMapping("/{id}/disease-mapping")
    public Result<List<DiseaseMappingResult>> getDiseaseMapping(@PathVariable Integer id,
                                                                  @RequestParam(required = false) String label) {
        VideoRecords record = videoRecordsMapper.selectById(id);
        if (record == null) {
            return Result.error("404", "识别记录不存在");
        }

        String cropType = record.getKind();
        if (StrUtil.isBlank(cropType)) {
            return Result.error("400", "识别记录缺少作物类型信息");
        }

        if (StrUtil.isBlank(label)) {
            return Result.error("400", "视频识别记录需要提供label参数");
        }

        List<DiseaseMappingResult> results = new ArrayList<>();
        try {
            DiseaseMappingResult mapping = diseaseMapperService.mapLabelToDisease(label, cropType);
            results.add(mapping);
            return Result.success(results);
        } catch (IllegalArgumentException e) {
            return Result.error("404", "病害映射失败: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("500", "病害映射失败: " + e.getMessage());
        }
    }
}
