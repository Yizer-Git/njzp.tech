package com.example.Kcsj.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class TaskCreationDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long fieldId;
    private String taskType;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date planStartTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date planEndTime;
    private Integer executorId;
    private String description;
    private String resourceUsage;
}

