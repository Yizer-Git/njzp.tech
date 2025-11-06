package com.example.Kcsj.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class DeviceControlRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long taskId;
    private String deviceId;

    /**
     * 硬件执行动作标识，兼容旧版字段 command
     */
    @JsonAlias("command")
    private String action;

    /**
     * 控制指令的值，例如 true/false 或持续时间
     */
    private Object value;

    private Map<String, Object> parameters;

    /**
     * 获取最终用于下发的指令标识
     */
    public String getResolvedAction() {
        if (action != null && !action.trim().isEmpty()) {
            return action;
        }
        if (parameters != null && parameters.containsKey("command")) {
            Object commandValue = parameters.get("command");
            return commandValue != null ? String.valueOf(commandValue) : null;
        }
        return null;
    }
}
