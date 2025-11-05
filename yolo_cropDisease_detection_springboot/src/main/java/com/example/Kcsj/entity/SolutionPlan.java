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
 * Solution recipe linking disease, crop and remedy guidance.
 */
@TableName("tb_solution")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SolutionPlan {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("disease_id")
    private Long diseaseId;
    @TableField("crop_id")
    private Long cropId;
    @TableField("crop_name")
    private String cropName;
    @TableField("remedy_id")
    private Long remedyId;
    @TableField("recommended_dosage")
    private BigDecimal recommendedDosage;
    @TableField("dosage_unit")
    private String dosageUnit;
    @TableField("application_stage")
    private String applicationStage;
    @TableField("application_timing")
    private String applicationTiming;
    private String notes;
    @TableField("weather_constraints")
    private String weatherConstraints;
    @TableField("expected_cost")
    private BigDecimal expectedCost;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("created_at")
    private Date createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("updated_at")
    private Date updatedAt;
}
