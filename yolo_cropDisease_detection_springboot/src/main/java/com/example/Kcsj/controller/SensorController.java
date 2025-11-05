package com.example.Kcsj.controller;

import com.example.Kcsj.common.Result;
import com.example.Kcsj.entity.SensorData;
import com.example.Kcsj.service.SensorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 传感器数据控制器
 * 提供传感器数据的接收、查询等功能
 */
@RestController
@RequestMapping("/api/sensor")
@RequiredArgsConstructor
@Slf4j
public class SensorController {

    private final SensorService sensorService;
    
    @Value("${sensor.default-device-id:DEVICE_001}")
    private String defaultDeviceId;

    /**
     * 接收物联网设备推送的传感器数据
     * POST /api/sensor/data
     */
    @PostMapping("/data")
    public Result<?> receiveSensorData(@RequestBody SensorData sensorData) {
        try {
            // 如果没有提供设备ID，使用默认值
            if (sensorData.getDeviceId() == null) {
                sensorData.setDeviceId(defaultDeviceId);
            }
            
            SensorData saved = sensorService.saveSensorData(sensorData);
            return Result.success(saved);
        } catch (IllegalArgumentException e) {
            log.error("传感器数据校验失败：{}", e.getMessage());
            return Result.error("-1", "数据校验失败：" + e.getMessage());
        } catch (Exception e) {
            log.error("保存传感器数据失败", e);
            return Result.error("-1", "保存数据失败：" + e.getMessage());
        }
    }

    /**
     * 获取最新的传感器数据
     * GET /api/sensor/latest?deviceId=DEVICE_001
     */
    @GetMapping("/latest")
    public Result<?> getLatestData(@RequestParam(required = false) String deviceId) {
        try {
            if (deviceId == null) {
                deviceId = defaultDeviceId;
            }
            
            SensorData latestData = sensorService.getLatestData(deviceId);
            if (latestData == null) {
                return Result.error("-1", "未找到传感器数据");
            }
            
            return Result.success(latestData);
        } catch (Exception e) {
            log.error("查询最新传感器数据失败", e);
            return Result.error("-1", "查询失败：" + e.getMessage());
        }
    }

    /**
     * 获取历史传感器数据
     * GET /api/sensor/history?deviceId=DEVICE_001&startTime=2025-01-01 00:00:00&endTime=2025-01-07 23:59:59
     */
    @GetMapping("/history")
    public Result<?> getHistoryData(
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        try {
            if (deviceId == null) {
                deviceId = defaultDeviceId;
            }
            
            // 如果没有指定时间范围，默认查询最近7天的数据
            if (startTime == null && endTime == null) {
                endTime = LocalDateTime.now();
                startTime = endTime.minusDays(7);
            }
            
            List<SensorData> historyData = sensorService.getHistoryData(deviceId, startTime, endTime);
            return Result.success(historyData);
        } catch (Exception e) {
            log.error("查询历史传感器数据失败", e);
            return Result.error("-1", "查询失败：" + e.getMessage());
        }
    }

    /**
     * 批量接收传感器数据（用于批量导入或设备批量上报）
     * POST /api/sensor/batch
     */
    @PostMapping("/batch")
    public Result<?> batchReceiveSensorData(@RequestBody List<SensorData> dataList) {
        try {
            if (dataList == null || dataList.isEmpty()) {
                return Result.error("-1", "数据列表不能为空");
            }
            
            // 为没有设备ID的数据设置默认值
            for (SensorData data : dataList) {
                if (data.getDeviceId() == null) {
                    data.setDeviceId(defaultDeviceId);
                }
            }
            
            boolean success = sensorService.saveBatch(dataList);
            if (success) {
                return Result.success("批量保存成功，共 " + dataList.size() + " 条数据");
            } else {
                return Result.error("-1", "批量保存失败");
            }
        } catch (Exception e) {
            log.error("批量保存传感器数据失败", e);
            return Result.error("-1", "批量保存失败：" + e.getMessage());
        }
    }
}


