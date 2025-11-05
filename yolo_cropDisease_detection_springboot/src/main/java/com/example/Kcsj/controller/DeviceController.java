package com.example.Kcsj.controller;

import com.example.Kcsj.common.Result;
import com.example.Kcsj.dto.DeviceControlRequest;
import com.example.Kcsj.dto.DeviceControlResponse;
import com.example.Kcsj.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/device")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @PostMapping("/execute")
    public Result<?> execute(@RequestBody DeviceControlRequest request) {
        DeviceControlResponse response = deviceService.executeControl(request);
        return Result.success(response);
    }
}

