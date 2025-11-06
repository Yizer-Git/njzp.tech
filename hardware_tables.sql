-- DDL for hardware integration tables
-- Ensure database is selected before running (e.g., USE cropdisease;)

-- 1. Sensor data table
CREATE TABLE IF NOT EXISTS `tb_sensor_data` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `device_id` VARCHAR(64) NOT NULL COMMENT '终端设备唯一ID',
  `temperature` DECIMAL(6,2) DEFAULT NULL COMMENT '空气温度(℃)',
  `humidity` DECIMAL(5,2) DEFAULT NULL COMMENT '空气湿度(%)',
  `soil_moisture` DECIMAL(5,2) DEFAULT NULL COMMENT '土壤湿度(%)',
  `light_intensity` DECIMAL(12,2) DEFAULT NULL COMMENT '光照强度(lux)',
  `water_level` TINYINT DEFAULT NULL COMMENT '水位状态(0=正常,1=缺水,2=水满)',
  `co2_level` DECIMAL(8,2) DEFAULT NULL COMMENT 'CO₂浓度(ppm)',
  `timestamp` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '采集时间',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_sensor_device_time` (`device_id`, `timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='传感器环境数据';

-- 2. Device control log table
CREATE TABLE IF NOT EXISTS `tb_device_control_log` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_id` BIGINT DEFAULT NULL COMMENT '关联任务ID',
  `device_id` VARCHAR(64) NOT NULL COMMENT '终端设备ID',
  `command` VARCHAR(64) NOT NULL COMMENT '执行指令标识',
  `response` TEXT COMMENT '指令响应/发送内容',
  `status` VARCHAR(32) DEFAULT 'SUCCESS' COMMENT '状态',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_control_device_time` (`device_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备控制日志';
