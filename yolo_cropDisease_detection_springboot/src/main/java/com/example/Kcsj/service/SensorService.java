package com.example.Kcsj.service;

import com.example.Kcsj.entity.SensorData;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 传感器数据服务接口
 */
public interface SensorService {
    
    /**
     * 接收并保存传感器数据
     */
    SensorData saveSensorData(SensorData sensorData);
    
    /**
     * 获取最新的传感器数据
     */
    SensorData getLatestData(String deviceId);
    
    /**
     * 获取指定时间范围内的历史数据
     */
    List<SensorData> getHistoryData(String deviceId, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 批量保存传感器数据
     */
    boolean saveBatch(List<SensorData> dataList);
}


