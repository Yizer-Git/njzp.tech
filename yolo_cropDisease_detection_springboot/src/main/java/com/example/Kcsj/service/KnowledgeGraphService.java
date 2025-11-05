package com.example.Kcsj.service;

import com.example.Kcsj.entity.TaskKnowledgeRelation;

import java.util.List;

public interface KnowledgeGraphService {

    /**
     * Build or refresh the knowledge relation entry for the specified task.
     */
    void buildRelation(Long taskId);

    /**
     * Query related entities for a disease.
     */
    List<TaskKnowledgeRelation> queryRelatedEntities(Long diseaseId);
}

