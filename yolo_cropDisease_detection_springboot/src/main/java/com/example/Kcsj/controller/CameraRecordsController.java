package com.example.Kcsj.controller;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.Kcsj.common.Result;
import com.example.Kcsj.dto.DiseaseMappingResult;
import com.example.Kcsj.entity.CameraRecords;
import com.example.Kcsj.mapper.CameraRecordsMapper;
import com.example.Kcsj.service.DiseaseMapperService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/cameraRecords")
public class CameraRecordsController {
    @Resource
    CameraRecordsMapper cameraRecordsMapper;

    @Resource
    DiseaseMapperService diseaseMapperService;

    @GetMapping("/all")
    public Result<?> GetAll() {
        return Result.success(cameraRecordsMapper.selectList(null));
    }
    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable int id) {
        System.out.println(id);
        return Result.success(cameraRecordsMapper.selectById(id));
    }

    @GetMapping
    public Result<?> findPage(@RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize,
                              @RequestParam(defaultValue = "") String search,
                              @RequestParam(defaultValue = "") String search1,
                              @RequestParam(defaultValue = "") String search3,
                              @RequestParam(defaultValue = "") String search2) {
        LambdaQueryWrapper<CameraRecords> wrapper = Wrappers.<CameraRecords>lambdaQuery();
        wrapper.orderByDesc(CameraRecords::getStartTime);
        if (StrUtil.isNotBlank(search)) {
            wrapper.like(CameraRecords::getUsername, search);
        }
        if (StrUtil.isNotBlank(search1)) {
            wrapper.like(CameraRecords::getKind, search1);
        }
        if (StrUtil.isNotBlank(search2)) {
            wrapper.like(CameraRecords::getWeight, search2);
        }
        if (StrUtil.isNotBlank(search3)) {
            wrapper.like(CameraRecords::getConf, search3);
        }
        Page<CameraRecords> Page = cameraRecordsMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return Result.success(Page);
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable int id) {
        cameraRecordsMapper.deleteById(id);
        return Result.success();
    }

    @PostMapping("/update")
    public Result<?> updates(@RequestBody CameraRecords cameraRecords) {
        cameraRecordsMapper.updateById(cameraRecords);
        return Result.success();
    }


    @PostMapping
    public Result<?> save(@RequestBody CameraRecords cameraRecords) {
        System.out.println(cameraRecords);
        cameraRecordsMapper.insert(cameraRecords);
        return Result.success();
    }

    /**
     * Get disease mapping for a camera record.
     * Since camera records don't store labels, label must be provided as query parameter.
     *
     * @param id    record ID
     * @param label detected disease label from camera analysis
     * @return disease mapping result
     */
    @GetMapping("/{id}/disease-mapping")
    public Result<List<DiseaseMappingResult>> getDiseaseMapping(@PathVariable Integer id,
                                                                  @RequestParam(required = false) String label) {
        CameraRecords record = cameraRecordsMapper.selectById(id);
        if (record == null) {
            return Result.error("404", "识别记录不存在");
        }

        String cropType = record.getKind();
        if (StrUtil.isBlank(cropType)) {
            return Result.error("400", "识别记录缺少作物类型信息");
        }

        if (StrUtil.isBlank(label)) {
            return Result.error("400", "摄像头识别记录需要提供label参数");
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
