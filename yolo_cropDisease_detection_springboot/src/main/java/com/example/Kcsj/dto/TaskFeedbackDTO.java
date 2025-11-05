package com.example.Kcsj.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class TaskFeedbackDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private BigDecimal actualDosage;
    private BigDecimal actualArea;
    private String feedbackText;
    private List<String> feedbackImages;
    private String resourceUsage;
}

