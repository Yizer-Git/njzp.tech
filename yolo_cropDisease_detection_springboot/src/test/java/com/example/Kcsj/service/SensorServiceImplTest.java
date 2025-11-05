package com.example.Kcsj.service;

import com.example.Kcsj.entity.SensorData;
import com.example.Kcsj.mapper.SensorDataMapper;
import com.example.Kcsj.service.impl.SensorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SensorServiceImpl#saveBatch(java.util.List)} to guard against regressions.
 */
@ExtendWith(MockitoExtension.class)
class SensorServiceImplTest {

    @Mock
    private SensorDataMapper sensorDataMapper;

    private SensorServiceImpl sensorService;

    @BeforeEach
    void setUp() {
        sensorService = new SensorServiceImpl(sensorDataMapper);
        // Ensure the inherited baseMapper is populated for any ServiceImpl internals.
        ReflectionTestUtils.setField(sensorService, "baseMapper", sensorDataMapper);
    }

    @Test
    void saveBatch_shouldValidateAndInsertEachRecord() {
        SensorData data1 = SensorData.builder()
                .deviceId("DEVICE_001")
                .temperature(new BigDecimal("26"))
                .humidity(new BigDecimal("55"))
                .build();
        SensorData data2 = SensorData.builder()
                .deviceId("DEVICE_002")
                .soilMoisture(new BigDecimal("48"))
                .lightIntensity(new BigDecimal("15000"))
                .build();

        when(sensorDataMapper.insert(any(SensorData.class))).thenReturn(1);

        boolean result = sensorService.saveBatch(Arrays.asList(data1, data2));

        assertTrue(result, "Batch save should succeed for valid records");
        assertNotNull(data1.getCreatedAt());
        assertNotNull(data1.getTimestamp());
        assertNotNull(data2.getCreatedAt());
        assertNotNull(data2.getTimestamp());
        verify(sensorDataMapper, times(2)).insert(any(SensorData.class));
    }

    @Test
    void saveBatch_shouldReturnFalseWhenListEmpty() {
        boolean result = sensorService.saveBatch(Collections.emptyList());

        assertFalse(result, "Empty batch should short-circuit without DB interaction");
        verifyNoInteractions(sensorDataMapper);
    }
}

