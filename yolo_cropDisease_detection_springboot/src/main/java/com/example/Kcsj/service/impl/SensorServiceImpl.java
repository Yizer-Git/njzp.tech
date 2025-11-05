package com.example.Kcsj.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.Kcsj.entity.SensorData;
import com.example.Kcsj.mapper.SensorDataMapper;
import com.example.Kcsj.service.SensorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * 传感器数据服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SensorServiceImpl extends ServiceImpl<SensorDataMapper, SensorData> implements SensorService {

    private final SensorDataMapper sensorDataMapper;

    @Override
    @Transactional
    public SensorData saveSensorData(SensorData sensorData) {
        // 数据校验
        validateSensorData(sensorData);
        
        // 设置创建时间
        if (sensorData.getCreatedAt() == null) {
            sensorData.setCreatedAt(new Date());
        }
        
        // 如果没有时间戳，使用当前时间
        if (sensorData.getTimestamp() == null) {
            sensorData.setTimestamp(LocalDateTime.now());
        }
        
        sensorDataMapper.insert(sensorData);
        log.info("保存传感器数据成功，设备ID: {}, 温度: {}℃, 湿度: {}%", 
                sensorData.getDeviceId(), 
                sensorData.getTemperature(), 
                sensorData.getHumidity());
        return sensorData;
    }

    @Override
    public SensorData getLatestData(String deviceId) {
        LambdaQueryWrapper<SensorData> wrapper = new LambdaQueryWrapper<SensorData>()
                .orderByDesc(SensorData::getTimestamp)
                .last("LIMIT 1");
        
        if (StrUtil.isNotBlank(deviceId)) {
            wrapper.eq(SensorData::getDeviceId, deviceId);
        }
        
        return sensorDataMapper.selectOne(wrapper);
    }

    @Override
    public List<SensorData> getHistoryData(String deviceId, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<SensorData> wrapper = new LambdaQueryWrapper<SensorData>()
                .orderByDesc(SensorData::getTimestamp);
        
        if (StrUtil.isNotBlank(deviceId)) {
            wrapper.eq(SensorData::getDeviceId, deviceId);
        }
        
        if (startTime != null) {
            wrapper.ge(SensorData::getTimestamp, startTime);
        }
        
        if (endTime != null) {
            wrapper.le(SensorData::getTimestamp, endTime);
        }
        
        return sensorDataMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public boolean saveBatch(List<SensorData> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return false;
        }

        // 逐条校验并补全默认值，随后调用 mapper 插入，避免递归调用 ServiceImpl#saveBatch
        for (SensorData data : dataList) {
            validateSensorData(data);
            if (data.getCreatedAt() == null) {
                data.setCreatedAt(new Date());
            }
            if (data.getTimestamp() == null) {
                data.setTimestamp(LocalDateTime.now());
            }
            sensorDataMapper.insert(data);
        }

        return true;
    }

    /**
     * 数据校验
     */
    private void validateSensorData(SensorData data) {
        if (data == null) {
            throw new IllegalArgumentException("传感器数据不能为空");
        }
        
        if (StrUtil.isBlank(data.getDeviceId())) {
            throw new IllegalArgumentException("设备ID不能为空");
        }
        
        // 温度范围校验：-40 ~ 60℃
        if (data.getTemperature() != null) {
            if (data.getTemperature().compareTo(new BigDecimal("-40")) < 0 || 
                data.getTemperature().compareTo(new BigDecimal("60")) > 0) {
                log.warn("温度数据异常：{}℃，设备ID: {}", data.getTemperature(), data.getDeviceId());
            }
        }
        
        // 湿度范围校验：0 ~ 100%
        if (data.getHumidity() != null) {
            if (data.getHumidity().compareTo(BigDecimal.ZERO) < 0 || 
                data.getHumidity().compareTo(new BigDecimal("100")) > 0) {
                log.warn("湿度数据异常：{}%，设备ID: {}", data.getHumidity(), data.getDeviceId());
            }
        }
        
        // 土壤墒情范围校验：0 ~ 100%
        if (data.getSoilMoisture() != null) {
            if (data.getSoilMoisture().compareTo(BigDecimal.ZERO) < 0 || 
                data.getSoilMoisture().compareTo(new BigDecimal("100")) > 0) {
                log.warn("土壤墒情数据异常：{}%，设备ID: {}", data.getSoilMoisture(), data.getDeviceId());
            }
        }
        
        // 光照强度范围校验：0 ~ 200000 lux
        if (data.getLightIntensity() != null) {
            if (data.getLightIntensity().compareTo(BigDecimal.ZERO) < 0 || 
                data.getLightIntensity().compareTo(new BigDecimal("200000")) > 0) {
                log.warn("光照强度数据异常：{} lux，设备ID: {}", data.getLightIntensity(), data.getDeviceId());
            }
        }
        
        // CO2浓度范围校验：200 ~ 5000 ppm
        if (data.getCo2Level() != null) {
            if (data.getCo2Level().compareTo(new BigDecimal("200")) < 0 || 
                data.getCo2Level().compareTo(new BigDecimal("5000")) > 0) {
                log.warn("CO2浓度数据异常：{} ppm，设备ID: {}", data.getCo2Level(), data.getDeviceId());
            }
        }
    }
}


