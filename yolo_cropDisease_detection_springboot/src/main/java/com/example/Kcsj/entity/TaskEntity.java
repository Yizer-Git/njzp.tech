package com.example.Kcsj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 农事任务实体
 */
@TableName("tb_farm_task")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("field_id")
    private Long fieldId;
    @TableField("task_type")
    private String taskType;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("plan_start_time")
    private Date planStartTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("plan_end_time")
    private Date planEndTime;
    @TableField("solution_id")
    private Long solutionId;
    @TableField("record_id")
    private Long recordId;
    @TableField("actual_dosage")
    private BigDecimal actualDosage;
    @TableField("actual_area")
    private BigDecimal actualArea;
    @TableField("executor_id")
    private Integer executorId;
    private String status;
    @TableField("resource_usage")
    private String resourceUsage;
    private String description;
    @TableField("feedback_text")
    private String feedbackText;
    @TableField("feedback_images")
    private String feedbackImages;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("progress_updated_at")
    private Date progressUpdatedAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("completed_at")
    private Date completedAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("archived_at")
    private Date archivedAt;
}
