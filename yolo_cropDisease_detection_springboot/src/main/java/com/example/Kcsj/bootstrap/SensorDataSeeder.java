package com.example.Kcsj.bootstrap;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.Kcsj.entity.SensorData;
import com.example.Kcsj.mapper.SensorDataMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Seeds the sensor data table with realistic demo data so that
 * the dashboard looks populated on first launch.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(10)
public class SensorDataSeeder implements CommandLineRunner {

    private final SensorDataMapper sensorDataMapper;

    @Value("${app.seed-sensor-data:true}")
    private boolean seedEnabled;

    @Override
    public void run(String... args) {
        if (!seedEnabled) {
            log.info("Sensor data seeding disabled via configuration.");
            return;
        }

        Integer existingRecords = sensorDataMapper.selectCount(Wrappers.emptyWrapper());
        if (existingRecords != null && existingRecords > 0) {
            log.info("Sensor data already present ({} records), skipping seed.", existingRecords);
            return;
        }

        List<SensorData> samples = buildSampleReadings();
        samples.forEach(sensorDataMapper::insert);
        log.info("Seeded {} sensor data records across {} demo devices.",
                samples.size(),
                samples.stream().map(SensorData::getDeviceId).distinct().count());
    }

    private List<SensorData> buildSampleReadings() {
        List<SensorData> results = new ArrayList<>();

        List<String> deviceIds = Arrays.asList("DEVICE_001", "DEVICE_002", "DEVICE_003");

        Map<String, Double> tempOffsets = new HashMap<>();
        tempOffsets.put("DEVICE_001", 25.0);
        tempOffsets.put("DEVICE_002", 22.5);
        tempOffsets.put("DEVICE_003", 24.0);

        Map<String, Double> humidityBase = new HashMap<>();
        humidityBase.put("DEVICE_001", 68.0);
        humidityBase.put("DEVICE_002", 72.0);
        humidityBase.put("DEVICE_003", 65.0);

        Map<String, Double> soilBase = new HashMap<>();
        soilBase.put("DEVICE_001", 48.0);
        soilBase.put("DEVICE_002", 55.0);
        soilBase.put("DEVICE_003", 52.0);

        LocalDateTime start = LocalDateTime.now()
                .minusHours(23)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (String deviceId : deviceIds) {
            for (int hour = 0; hour < 24; hour++) {
                LocalDateTime timestamp = start.plusHours(hour);
                double hourFactor = Math.sin((hour / 24.0) * Math.PI * 1.6); // daytime bump

                double temperature = tempOffsets.get(deviceId) + hourFactor * 4 + noise(random, 0.6);
                double humidity = humidityBase.get(deviceId) - hourFactor * 8 + noise(random, 2.0);
                double soilMoisture = soilBase.get(deviceId) - hourFactor * 5 + noise(random, 1.5);

                double lightIntensity = Math.max(0, (hour >= 6 && hour <= 18)
                        ? 350 + hourFactor * 900 + random.nextDouble(100, 180)
                        : random.nextDouble(5, 20));

                int waterLevel = (hour % 12 == 0) ? 2 : (soilMoisture < 40 ? 1 : 0);
                double co2Level = 420 + hourFactor * 15 + noise(random, 4.0);

                results.add(SensorData.builder()
                        .deviceId(deviceId)
                        .temperature(toBigDecimal(temperature))
                        .humidity(toBigDecimal(clamp(humidity, 45, 95)))
                        .soilMoisture(toBigDecimal(clamp(soilMoisture, 30, 80)))
                        .lightIntensity(toBigDecimal(lightIntensity))
                        .waterLevel(waterLevel)
                        .co2Level(toBigDecimal(clamp(co2Level, 380, 520)))
                        .timestamp(timestamp)
                        .createdAt(toDate(timestamp))
                        .build());
            }
        }

        return results;
    }

    private double noise(ThreadLocalRandom random, double amplitude) {
        return random.nextDouble(-amplitude, amplitude);
    }

    private BigDecimal toBigDecimal(double value) {
        return BigDecimal.valueOf(Math.round(value * 100.0) / 100.0);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private Date toDate(LocalDateTime time) {
        return Date.from(time.atZone(ZoneId.systemDefault()).toInstant());
    }
}
