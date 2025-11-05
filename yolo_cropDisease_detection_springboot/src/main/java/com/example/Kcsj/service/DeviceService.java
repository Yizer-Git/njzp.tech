package com.example.Kcsj.service;

import com.example.Kcsj.dto.DeviceControlRequest;
import com.example.Kcsj.dto.DeviceControlResponse;

public interface DeviceService {
    DeviceControlResponse executeControl(DeviceControlRequest request);
}

