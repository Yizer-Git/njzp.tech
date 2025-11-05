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
 * Remedy catalog entry containing agronomic and economic attributes.
 */
@TableName("tb_remedy")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Remedy {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("remedy_code")
    private String remedyCode;
    @TableField("remedy_name")
    private String remedyName;
    @TableField("active_ingredient")
    private String activeIngredient;
    @TableField("target_pathogen")
    private String targetPathogen;
    private String formulation;
    @TableField("safe_dosage")
    private BigDecimal safeDosage;
    @TableField("dosage_unit")
    private String dosageUnit;
    @TableField("interval_days")
    private Integer intervalDays;
    @TableField("application_method")
    private String applicationMethod;
    @TableField("safety_interval_days")
    private Integer safetyIntervalDays;
    @TableField("max_applications_per_season")
    private Integer maxApplicationsPerSeason;
    private String caution;
    @TableField("cost_per_unit")
    private BigDecimal costPerUnit;
    private String currency;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("last_price_update")
    private Date lastPriceUpdate;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("created_at")
    private Date createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("updated_at")
    private Date updatedAt;
}
