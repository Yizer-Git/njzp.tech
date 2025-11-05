package com.example.Kcsj.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.Kcsj.dto.TaskChainDTO;
import com.example.Kcsj.dto.TaskCreationDTO;
import com.example.Kcsj.dto.TaskFeedbackDTO;
import com.example.Kcsj.entity.ImgRecords;
import com.example.Kcsj.entity.SolutionPlan;
import com.example.Kcsj.entity.TaskEntity;
import com.example.Kcsj.mapper.ImgRecordsMapper;
import com.example.Kcsj.mapper.SolutionPlanMapper;
import com.example.Kcsj.mapper.TaskMapper;
import com.example.Kcsj.service.KnowledgeGraphService;
import com.example.Kcsj.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskServiceImpl implements TaskService {

    private static final Map<String, Set<String>> STATE_MACHINE;

    static {
        Map<String, Set<String>> map = new HashMap<>();
        map.put("planned", new HashSet<>(Collections.singletonList("assigned")));
        map.put("assigned", new HashSet<>(Collections.singletonList("in_progress")));
        map.put("in_progress", new HashSet<>(Collections.singletonList("completed")));
        map.put("completed", new HashSet<>(Collections.singletonList("archived")));
        map.put("archived", Collections.emptySet());
        STATE_MACHINE = Collections.unmodifiableMap(map);
    }

    private final TaskMapper taskMapper;
    private final SolutionPlanMapper solutionPlanMapper;
    private final ImgRecordsMapper imgRecordsMapper;
    private final KnowledgeGraphService knowledgeGraphService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFromSolution(Long solutionId, Long recordId, TaskCreationDTO payload) {
        if (solutionId == null) {
            throw new IllegalArgumentException("solutionId 不能为空");
        }
        SolutionPlan solutionPlan = solutionPlanMapper.selectById(solutionId);
        if (solutionPlan == null) {
            throw new IllegalArgumentException("未找到对应的防治方案，ID=" + solutionId);
        }

        Date now = new Date();
        TaskEntity task = TaskEntity.builder()
                .fieldId(payload.getFieldId())
                .taskType(StrUtil.blankToDefault(payload.getTaskType(), "solution_application"))
                .planStartTime(payload.getPlanStartTime())
                .planEndTime(payload.getPlanEndTime())
                .executorId(payload.getExecutorId())
                .status("planned")
                .resourceUsage(payload.getResourceUsage())
                .description(payload.getDescription())
                .solutionId(solutionId)
                .recordId(recordId)
                .progressUpdatedAt(now)
                .build();
        taskMapper.insert(task);

        // 绑定识别记录
        if (recordId != null) {
            ImgRecords record = imgRecordsMapper.selectById(recordId.intValue());
            if (record != null) {
                record.setTaskId(task.getId().intValue());
                imgRecordsMapper.updateById(record);
            }
        }

        // 构建知识图谱关联
        try {
            knowledgeGraphService.buildRelation(task.getId());
        } catch (Exception ex) {
            log.warn("知识关联构建失败: {}", ex.getMessage());
        }

        return task.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitFeedback(Long taskId, TaskFeedbackDTO feedback) {
        TaskEntity task = requireTask(taskId);

        task.setActualDosage(feedback.getActualDosage());
        task.setActualArea(feedback.getActualArea());
        task.setFeedbackText(feedback.getFeedbackText());
        if (CollUtil.isNotEmpty(feedback.getFeedbackImages())) {
            task.setFeedbackImages(JSON.toJSONString(feedback.getFeedbackImages()));
        } else {
            task.setFeedbackImages(null);
        }
        if (StrUtil.isNotBlank(feedback.getResourceUsage())) {
            task.setResourceUsage(feedback.getResourceUsage());
        }
        Date now = new Date();
        task.setCompletedAt(now);
        task.setProgressUpdatedAt(now);

        if (!Objects.equals(task.getStatus(), "completed") && !Objects.equals(task.getStatus(), "archived")) {
            task.setStatus("completed");
        }

        taskMapper.updateById(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void archiveTask(Long taskId) {
        TaskEntity task = requireTask(taskId);
        if (!Objects.equals(task.getStatus(), "completed") && !Objects.equals(task.getStatus(), "archived")) {
            throw new IllegalStateException("仅已完成的任务才允许归档");
        }
        Date now = new Date();
        task.setStatus("archived");
        task.setArchivedAt(now);
        task.setProgressUpdatedAt(now);
        taskMapper.updateById(task);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskChainDTO getTaskChain(Long taskId) {
        TaskEntity task = requireTask(taskId);
        SolutionPlan solutionPlan = null;
        if (task.getSolutionId() != null) {
            solutionPlan = solutionPlanMapper.selectById(task.getSolutionId());
        }
        ImgRecords record = null;
        if (task.getRecordId() != null) {
            record = imgRecordsMapper.selectById(task.getRecordId().intValue());
        } else if (task.getId() != null) {
            record = imgRecordsMapper.selectOne(
                    Wrappers.<ImgRecords>lambdaQuery().eq(ImgRecords::getTaskId, task.getId().intValue()).last("LIMIT 1"));
        }

        TaskChainDTO.FeedbackSummary feedbackSummary = new TaskChainDTO.FeedbackSummary();
        feedbackSummary.setActualDosage(task.getActualDosage());
        feedbackSummary.setActualArea(task.getActualArea());
        feedbackSummary.setFeedbackText(task.getFeedbackText());
        if (StrUtil.isNotBlank(task.getFeedbackImages())) {
            List<String> images = JSONArray.parseArray(task.getFeedbackImages(), String.class);
            feedbackSummary.setFeedbackImages(images);
        }
        feedbackSummary.setStatus(task.getStatus());

        TaskChainDTO dto = new TaskChainDTO();
        dto.setTask(task);
        dto.setSolution(solutionPlan);
        dto.setRecord(record);
        dto.setFeedback(feedbackSummary);
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskEntity updateTaskStatus(Long taskId, String newStatus) {
        TaskEntity task = requireTask(taskId);
        if (StrUtil.isBlank(newStatus)) {
            throw new IllegalArgumentException("新状态不能为空");
        }
        newStatus = newStatus.trim().toLowerCase();

        String currentStatus = StrUtil.blankToDefault(task.getStatus(), "planned");
        if (Objects.equals(currentStatus, newStatus)) {
            return task;
        }

        Set<String> allowed = STATE_MACHINE.getOrDefault(currentStatus, Collections.emptySet());
        if (!allowed.contains(newStatus)) {
            throw new IllegalStateException(String.format("任务状态不允许从 %s 转为 %s", currentStatus, newStatus));
        }

        Date now = new Date();
        task.setStatus(newStatus);
        if (Objects.equals(newStatus, "completed") && task.getCompletedAt() == null) {
            task.setCompletedAt(now);
        }
        if (Objects.equals(newStatus, "archived")) {
            task.setArchivedAt(now);
        }
        task.setProgressUpdatedAt(now);

        taskMapper.updateById(task);
        return task;
    }

    private TaskEntity requireTask(Long taskId) {
        TaskEntity task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在，ID=" + taskId);
        }
        return task;
    }
}

