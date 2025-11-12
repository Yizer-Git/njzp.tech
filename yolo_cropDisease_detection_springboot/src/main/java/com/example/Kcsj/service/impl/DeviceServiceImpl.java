package com.example.Kcsj.service.impl;

import cn.hutool.core.util.StrUtil;
import com.example.Kcsj.config.MqttConfig;
import com.example.Kcsj.dto.DeviceControlRequest;
import com.example.Kcsj.dto.DeviceControlResponse;
import com.example.Kcsj.entity.DeviceControlLog;
import com.example.Kcsj.mapper.DeviceControlLogMapper;
import com.example.Kcsj.service.DeviceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceServiceImpl implements DeviceService {

    private final DeviceControlLogMapper deviceControlLogMapper;
    private final ObjectMapper objectMapper;
    private final Optional<MqttConfig.MqttGateway> mqttGateway;

    @Value("${mqtt.topic.control-prefix:device/control/}")
    private String controlTopicPrefix;

    @Value("${mqtt.enabled:true}")
    private boolean mqttEnabled;

    @Override
    public DeviceControlResponse executeControl(DeviceControlRequest request) {
        if (request == null || StrUtil.isBlank(request.getDeviceId())) {
            throw new IllegalArgumentException("设备控制请求参数不完整");
        }

        String resolvedAction = request.getResolvedAction();
        if (StrUtil.isBlank(resolvedAction)) {
            throw new IllegalArgumentException("缺少控制指令标识 action");
        }

        Map<String, Object> payload = buildPayload(request, resolvedAction);
        String topic = buildControlTopic(request.getDeviceId());

        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            log.error("序列化控制指令失败", ex);
            throw new IllegalStateException("序列化失败: " + ex.getMessage(), ex);
        }

        if (!mqttEnabled || !mqttGateway.isPresent()) {
            log.warn("MQTT 未启用或未配置，跳过指令下发。topic={}, payload={}", topic, jsonPayload);
            persistControlLog(request, resolvedAction, jsonPayload, "SKIPPED");
            return DeviceControlResponse.builder()
                    .status("SKIPPED")
                    .message(String.format("MQTT 未启用，指令 [%s] 未发送但已记录", resolvedAction))
                    .deviceId(request.getDeviceId())
                    .timestamp(new Date())
                    .build();
        }

        try {
            mqttGateway.get().sendToMqtt(topic, jsonPayload);
            log.info("MQTT 指令已下发，topic={}, payload={}", topic, jsonPayload);
            persistControlLog(request, resolvedAction, jsonPayload, "SUCCESS");

            return DeviceControlResponse.builder()
                    .status("SUCCESS")
                    .message(String.format("指令 [%s] 已发送至设备 %s", resolvedAction, request.getDeviceId()))
                    .deviceId(request.getDeviceId())
                    .timestamp(new Date())
                    .build();
        } catch (Exception ex) {
            log.error("发送 MQTT 控制指令失败", ex);
            persistControlLog(request, resolvedAction, jsonPayload, "FAILED");
            throw new IllegalStateException("发送 MQTT 指令失败: " + ex.getMessage(), ex);
        }
    }

    private Map<String, Object> buildPayload(DeviceControlRequest request, String resolvedAction) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("deviceId", request.getDeviceId());
        payload.put("action", resolvedAction);
        if (request.getValue() != null) {
            payload.put("value", request.getValue());
        } else if (request.getParameters() != null && request.getParameters().containsKey("value")) {
            payload.put("value", request.getParameters().get("value"));
        }
        if (request.getParameters() != null && !request.getParameters().isEmpty()) {
            payload.put("parameters", request.getParameters());
        }
        return payload;
    }

    private String buildControlTopic(String deviceId) {
        return controlTopicPrefix + deviceId;
    }

    private void persistControlLog(DeviceControlRequest request, String resolvedAction, String payload, String status) {
        DeviceControlLog logEntry = DeviceControlLog.builder()
                .taskId(request.getTaskId())
                .deviceId(request.getDeviceId())
                .command(resolvedAction)
                .response(payload)
                .status(status)
                .createdAt(new Date())
                .build();
        deviceControlLogMapper.insert(logEntry);
    }
}
