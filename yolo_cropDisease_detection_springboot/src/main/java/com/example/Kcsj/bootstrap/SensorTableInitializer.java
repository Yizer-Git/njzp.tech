package com.example.Kcsj.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ensures the tb_sensor_data table contains the columns that the application expects.
 * This provides a light-weight safeguard for existing deployments that may not run the latest DDL.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SensorTableInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Value("${app.sensor-table-auto-update:true}")
    private boolean autoUpdate;

    private static final String TABLE_NAME = "tb_sensor_data";

    @Override
    public void run(String... args) {
        if (!autoUpdate) {
            log.info("Sensor table auto-update disabled via configuration.");
            return;
        }

        try {
            Map<String, String> requiredColumns = buildColumnDefinitions();
            for (Map.Entry<String, String> entry : requiredColumns.entrySet()) {
                ensureColumnExists(entry.getKey(), entry.getValue());
            }
        } catch (Exception ex) {
            log.error("Failed to verify tb_sensor_data schema. The application may not work as expected.", ex);
        }
    }

    private Map<String, String> buildColumnDefinitions() {
        Map<String, String> columns = new LinkedHashMap<>();
        columns.put("temperature", "ALTER TABLE " + TABLE_NAME + " ADD COLUMN temperature DECIMAL(6,2) DEFAULT NULL COMMENT '空气温度(℃)' AFTER device_id");
        columns.put("humidity", "ALTER TABLE " + TABLE_NAME + " ADD COLUMN humidity DECIMAL(5,2) DEFAULT NULL COMMENT '空气湿度(%)' AFTER temperature");
        columns.put("soil_moisture", "ALTER TABLE " + TABLE_NAME + " ADD COLUMN soil_moisture DECIMAL(5,2) DEFAULT NULL COMMENT '土壤湿度(%)' AFTER humidity");
        columns.put("light_intensity", "ALTER TABLE " + TABLE_NAME + " ADD COLUMN light_intensity DECIMAL(12,2) DEFAULT NULL COMMENT '光照强度(lux)' AFTER soil_moisture");
        columns.put("water_level", "ALTER TABLE " + TABLE_NAME + " ADD COLUMN water_level TINYINT DEFAULT NULL COMMENT '水位状态(0=正常,1=缺水,2=水满)' AFTER light_intensity");
        columns.put("co2_level", "ALTER TABLE " + TABLE_NAME + " ADD COLUMN co2_level DECIMAL(8,2) DEFAULT NULL COMMENT 'CO₂浓度(ppm)' AFTER water_level");
        columns.put("timestamp", "ALTER TABLE " + TABLE_NAME + " ADD COLUMN `timestamp` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '采集时间' AFTER co2_level");
        columns.put("created_at", "ALTER TABLE " + TABLE_NAME + " ADD COLUMN created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间' AFTER `timestamp`");
        return columns;
    }

    private void ensureColumnExists(String columnName, String alterSql) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                Integer.class,
                TABLE_NAME,
                columnName
        );

        if (count == null || count == 0) {
            log.warn("Column '{}' missing in table '{}', applying patch...", columnName, TABLE_NAME);
            jdbcTemplate.execute(alterSql);
            log.info("Added column '{}' to table '{}'.", columnName, TABLE_NAME);
        }
    }
}

