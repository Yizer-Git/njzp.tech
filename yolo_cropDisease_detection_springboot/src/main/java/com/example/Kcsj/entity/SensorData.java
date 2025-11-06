package com.example.Kcsj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 传感器环境数据实体类
 * 包含温度、湿度、土壤墒情、光照强度、CO2浓度等环境监测数据
 */
@TableName("tb_sensor_data")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SensorData {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("device_id")
    private String deviceId;
    
    /**
     * 温度（℃）
     */
    @JsonProperty("airTemperature")
    @JsonAlias("temperature")
    private BigDecimal temperature;
    
    /**
     * 湿度（%）
     */
    @JsonProperty("airHumidity")
    @JsonAlias("humidity")
    private BigDecimal humidity;
    
    /**
     * 土壤墒情（%）
     */
    @TableField("soil_moisture")
    @JsonProperty("soilHumidity")
    @JsonAlias("soilMoisture")
    private BigDecimal soilMoisture;
    
    /**
     * 光照强度（lux）
     */
    @TableField("light_intensity")
    private BigDecimal lightIntensity;

    /**
     * 水位状态
     * 0=正常, 1=缺水, 2=水满
     */
    @TableField("water_level")
    private Integer waterLevel;
    
    /**
     * CO2浓度（ppm）
     */
    @TableField("co2_level")
    private BigDecimal co2Level;
    
    /**
     * 采集时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime timestamp;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("created_at")
    private Date createdAt;
}

