package com.example.Kcsj.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.example.Kcsj.dto.DeviceControlRequest;
import com.example.Kcsj.dto.DeviceControlResponse;
import com.example.Kcsj.entity.DeviceControlLog;
import com.example.Kcsj.mapper.DeviceControlLogMapper;
import com.example.Kcsj.service.DeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceServiceImpl implements DeviceService {

    private final DeviceControlLogMapper deviceControlLogMapper;

    @Override
    public DeviceControlResponse executeControl(DeviceControlRequest request) {
        if (request == null || StrUtil.isBlank(request.getDeviceId())) {
            throw new IllegalArgumentException("设备控制请求参数不完整");
        }

        String resolvedCommand = StrUtil.blankToDefault(request.getCommand(), "APPLY_SOLUTION");
        String responseMessage = String.format("设备 %s 已接收指令 %s", request.getDeviceId(), resolvedCommand);
        DeviceControlLog logEntry = DeviceControlLog.builder()
                .taskId(request.getTaskId())
                .deviceId(request.getDeviceId())
                .command(resolvedCommand)
                .response(buildResponsePayload(request))
                .status("SUCCESS")
                .createdAt(new Date())
                .build();
        deviceControlLogMapper.insert(logEntry);

        log.info("设备控制模拟完成：{}", logEntry);

        return DeviceControlResponse.builder()
                .status("SUCCESS")
                .message(responseMessage)
                .deviceId(request.getDeviceId())
                .timestamp(new Date())
                .build();
    }

    private String buildResponsePayload(DeviceControlRequest request) {
        return JSON.toJSONString(request.getParameters());
    }
}

