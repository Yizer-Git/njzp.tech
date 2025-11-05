package com.example.Kcsj.dto;

import com.example.Kcsj.entity.ImgRecords;
import com.example.Kcsj.entity.SolutionPlan;
import com.example.Kcsj.entity.TaskEntity;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class TaskChainDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private TaskEntity task;
    private SolutionPlan solution;
    private ImgRecords record;
    private FeedbackSummary feedback;

    @Data
    public static class FeedbackSummary implements Serializable {
        private static final long serialVersionUID = 1L;
        private BigDecimal actualDosage;
        private BigDecimal actualArea;
        private String feedbackText;
        private List<String> feedbackImages;
        private String status;
    }
}

