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

@TableName("tb_task_knowledge_relation")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskKnowledgeRelation {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("task_id")
    private Long taskId;
    @TableField("disease_id")
    private Long diseaseId;
    @TableField("crop_id")
    private Long cropId;
    @TableField("remedy_id")
    private Long remedyId;
    @TableField("relation_type")
    private String relationType;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("created_at")
    private Date createdAt;
}

