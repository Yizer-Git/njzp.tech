package com.example.Kcsj.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.Kcsj.entity.SolutionPlan;
import com.example.Kcsj.entity.TaskEntity;
import com.example.Kcsj.entity.TaskKnowledgeRelation;
import com.example.Kcsj.mapper.SolutionPlanMapper;
import com.example.Kcsj.mapper.TaskKnowledgeRelationMapper;
import com.example.Kcsj.mapper.TaskMapper;
import com.example.Kcsj.service.KnowledgeGraphService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeGraphServiceImpl implements KnowledgeGraphService {

    private final TaskMapper taskMapper;
    private final SolutionPlanMapper solutionPlanMapper;
    private final TaskKnowledgeRelationMapper relationMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void buildRelation(Long taskId) {
        if (taskId == null) {
            return;
        }
        TaskEntity task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在，无法建立知识关联");
        }

        // 清理旧记录，避免重复
        relationMapper.delete(Wrappers.<TaskKnowledgeRelation>lambdaQuery().eq(TaskKnowledgeRelation::getTaskId, taskId));

        if (task.getSolutionId() == null) {
            log.debug("任务 {} 未关联方案，跳过知识映射", taskId);
            return;
        }
        SolutionPlan solution = solutionPlanMapper.selectById(task.getSolutionId());
        if (solution == null) {
            log.debug("任务 {} 的方案 {} 已不存在，跳过知识映射", taskId, task.getSolutionId());
            return;
        }

        TaskKnowledgeRelation relation = TaskKnowledgeRelation.builder()
                .taskId(taskId)
                .diseaseId(solution.getDiseaseId())
                .cropId(solution.getCropId())
                .remedyId(solution.getRemedyId())
                .relationType("solution_link")
                .createdAt(new Date())
                .build();
        relationMapper.insert(relation);
    }

    @Override
    public List<TaskKnowledgeRelation> queryRelatedEntities(Long diseaseId) {
        if (diseaseId == null) {
            return Collections.emptyList();
        }
        return relationMapper.selectList(
                Wrappers.<TaskKnowledgeRelation>lambdaQuery().eq(TaskKnowledgeRelation::getDiseaseId, diseaseId));
    }
}

