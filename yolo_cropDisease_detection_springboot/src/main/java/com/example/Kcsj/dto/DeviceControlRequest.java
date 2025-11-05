package com.example.Kcsj.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class DeviceControlRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long taskId;
    private String deviceId;
    private String command;
    private Map<String, Object> parameters;
}

