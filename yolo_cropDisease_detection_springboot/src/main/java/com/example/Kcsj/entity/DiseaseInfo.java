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

import java.util.Date;

/**
 * Structured disease knowledge base entry.
 */
@TableName("disease_knowledge_data")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DiseaseInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("disease_code")
    private String diseaseCode;
    @TableField("disease_name")
    private String diseaseName;
    @TableField("crop_id")
    private Long cropId;
    @TableField("crop_name")
    private String cropName;
    private String description;
    @TableField("symptom_summary")
    private String symptomSummary;
    @TableField("pathogen_type")
    private String pathogenType;
    @TableField("risk_level")
    private String riskLevel;
    @TableField("climate_risk_factors")
    private String climateRiskFactors;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("created_at")
    private Date createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("updated_at")
    private Date updatedAt;
}
