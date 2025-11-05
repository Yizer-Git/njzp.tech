package com.example.Kcsj.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.Kcsj.common.Result;
import com.example.Kcsj.dto.TaskChainDTO;
import com.example.Kcsj.dto.TaskCreationDTO;
import com.example.Kcsj.dto.TaskFeedbackDTO;
import com.example.Kcsj.entity.TaskEntity;
import com.example.Kcsj.mapper.TaskMapper;
import com.example.Kcsj.service.TaskService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    @Resource
    private TaskMapper taskMapper;

    @Resource
    private TaskService taskService;

    @GetMapping
    public Result<?> list(@RequestParam(defaultValue = "1") Integer pageNum,
                          @RequestParam(defaultValue = "10") Integer pageSize,
                          @RequestParam(required = false) String status,
                          @RequestParam(required = false) Integer executorId,
                          @RequestParam(required = false) String taskType,
                          @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<TaskEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.orderByDesc(TaskEntity::getPlanStartTime);
        if (StrUtil.isNotBlank(status)) {
            wrapper.eq(TaskEntity::getStatus, status);
        }
        if (executorId != null) {
            wrapper.eq(TaskEntity::getExecutorId, executorId);
        }
        if (StrUtil.isNotBlank(taskType)) {
            wrapper.eq(TaskEntity::getTaskType, taskType);
        }
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(TaskEntity::getDescription, keyword)
                    .or().like(TaskEntity::getTaskType, keyword)
                    .or().like(TaskEntity::getFieldId, keyword));
        }
        Page<TaskEntity> page = taskMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return Result.success(page);
    }

    @GetMapping("/stats")
    public Result<?> stats() {
        List<TaskEntity> tasks = taskMapper.selectList(null);
        Map<String, Long> aggregated = new HashMap<>();
        for (TaskEntity task : tasks) {
            String status = StrUtil.blankToDefault(task.getStatus(), "planned");
            aggregated.merge(status, 1L, Long::sum);
        }
        return Result.success(aggregated);
    }

    @GetMapping("/chain-stats")
    public Result<?> chainStats() {
        List<TaskEntity> tasks = taskMapper.selectList(null);
        Map<String, Long> statusBreakdown = new HashMap<>();
        long completed = 0L;
        long archived = 0L;
        long solutionApplied = 0L;
        long durationCount = 0L;
        double durationTotal = 0D;

        for (TaskEntity task : tasks) {
            String status = StrUtil.blankToDefault(task.getStatus(), "planned");
            statusBreakdown.merge(status, 1L, Long::sum);
            if ("completed".equalsIgnoreCase(status)) {
                completed++;
            } else if ("archived".equalsIgnoreCase(status)) {
                archived++;
            }
            if (task.getSolutionId() != null) {
                solutionApplied++;
            }
            if (task.getCompletedAt() != null && task.getPlanStartTime() != null) {
                long diff = task.getCompletedAt().getTime() - task.getPlanStartTime().getTime();
                if (diff > 0) {
                    durationTotal += diff / (1000D * 60 * 60);
                    durationCount++;
                }
            }
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("totalTasks", tasks.size());
        payload.put("completedTasks", completed);
        payload.put("archivedTasks", archived);
        payload.put("solutionApplications", solutionApplied);
        payload.put("avgDurationHours", durationCount == 0 ? 0D : durationTotal / durationCount);
        payload.put("statusBreakdown", statusBreakdown);

        return Result.success(payload);
    }

    @PostMapping
    public Result<?> create(@RequestBody TaskEntity task) {
        if (StrUtil.isBlank(task.getStatus())) {
            task.setStatus("planned");
        }
        task.setProgressUpdatedAt(new Date());
        taskMapper.insert(task);
        return Result.success(task);
    }

    @PostMapping("/createFromSolution")
    public Result<?> createFromSolution(@RequestBody CreateFromSolutionRequest request) {
        if (request == null || request.getSolutionId() == null || request.getTask() == null) {
            return Result.error("-1", "方案或任务参数不能为空");
        }
        Long taskId = taskService.createFromSolution(request.getSolutionId(), request.getRecordId(), request.getTask());
        TaskEntity created = taskMapper.selectById(taskId);
        return Result.success(created);
    }

    @PostMapping("/{id}/feedback")
    public Result<?> submitFeedback(@PathVariable Long id, @RequestBody TaskFeedbackRequest request) {
        if (request == null) {
            return Result.error("-1", "反馈数据不能为空");
        }
        taskService.submitFeedback(id, request.toDto());
        TaskEntity updated = taskMapper.selectById(id);
        return Result.success(updated);
    }

    @PutMapping("/{id}/archive")
    public Result<?> archiveTask(@PathVariable Long id) {
        taskService.archiveTask(id);
        return Result.success(taskMapper.selectById(id));
    }

    @GetMapping("/{id}/chain")
    public Result<?> getTaskChain(@PathVariable Long id) {
        TaskChainDTO chain = taskService.getTaskChain(id);
        return Result.success(chain);
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody TaskEntity payload) {
        TaskEntity existing = taskMapper.selectById(id);
        if (existing == null) {
            return Result.error("-1", "任务不存在");
        }
        if (payload.getFieldId() != null) {
            existing.setFieldId(payload.getFieldId());
        }
        if (StrUtil.isNotBlank(payload.getTaskType())) {
            existing.setTaskType(payload.getTaskType());
        }
        if (payload.getPlanStartTime() != null) {
            existing.setPlanStartTime(payload.getPlanStartTime());
        }
        if (payload.getPlanEndTime() != null) {
            existing.setPlanEndTime(payload.getPlanEndTime());
        }
        if (payload.getExecutorId() != null) {
            existing.setExecutorId(payload.getExecutorId());
        }
        if (StrUtil.isNotBlank(payload.getStatus())) {
            existing.setStatus(payload.getStatus());
        }
        if (StrUtil.isNotBlank(payload.getResourceUsage())) {
            existing.setResourceUsage(payload.getResourceUsage());
        }
        if (StrUtil.isNotBlank(payload.getDescription())) {
            existing.setDescription(payload.getDescription());
        }
        existing.setProgressUpdatedAt(new Date());
        taskMapper.updateById(existing);
        return Result.success(existing);
    }

    @PutMapping("/{id}/assign")
    public Result<?> assign(@PathVariable Long id, @RequestBody AssignRequest request) {
        TaskEntity existing = taskMapper.selectById(id);
        if (existing == null) {
            return Result.error("-1", "任务不存在");
        }
        existing.setExecutorId(request.getExecutorId());
        if (StrUtil.isNotBlank(request.getStatus())) {
            existing.setStatus(request.getStatus());
        } else if (Objects.equals(existing.getStatus(), "planned")) {
            existing.setStatus("assigned");
        }
        existing.setProgressUpdatedAt(new Date());
        taskMapper.updateById(existing);
        return Result.success(existing);
    }

    @PutMapping("/{id}/status")
    public Result<?> updateStatus(@PathVariable Long id, @RequestBody StatusRequest request) {
        if (StrUtil.isBlank(request.getStatus())) {
            return Result.error("-1", "状态不能为空");
        }
        try {
            TaskEntity updated = taskService.updateTaskStatus(id, request.getStatus());
            if (StrUtil.isNotBlank(request.getResourceUsage())) {
                updated.setResourceUsage(request.getResourceUsage());
                taskMapper.updateById(updated);
            }
            return Result.success(updated);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return Result.error("-1", ex.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        taskMapper.deleteById(id);
        return Result.success();
    }

    public static class CreateFromSolutionRequest {
        private Long solutionId;
        private Long recordId;
        private TaskCreationDTO task;

        public Long getSolutionId() {
            return solutionId;
        }

        public void setSolutionId(Long solutionId) {
            this.solutionId = solutionId;
        }

        public Long getRecordId() {
            return recordId;
        }

        public void setRecordId(Long recordId) {
            this.recordId = recordId;
        }

        public TaskCreationDTO getTask() {
            return task;
        }

        public void setTask(TaskCreationDTO task) {
            this.task = task;
        }
    }

    public static class TaskFeedbackRequest {
        private BigDecimal actualDosage;
        private BigDecimal actualArea;
        private String feedbackText;
        private List<String> feedbackImages;
        private String resourceUsage;

        public BigDecimal getActualDosage() {
            return actualDosage;
        }

        public void setActualDosage(BigDecimal actualDosage) {
            this.actualDosage = actualDosage;
        }

        public BigDecimal getActualArea() {
            return actualArea;
        }

        public void setActualArea(BigDecimal actualArea) {
            this.actualArea = actualArea;
        }

        public String getFeedbackText() {
            return feedbackText;
        }

        public void setFeedbackText(String feedbackText) {
            this.feedbackText = feedbackText;
        }

        public List<String> getFeedbackImages() {
            return feedbackImages;
        }

        public void setFeedbackImages(List<String> feedbackImages) {
            this.feedbackImages = feedbackImages;
        }

        public String getResourceUsage() {
            return resourceUsage;
        }

        public void setResourceUsage(String resourceUsage) {
            this.resourceUsage = resourceUsage;
        }

        public TaskFeedbackDTO toDto() {
            TaskFeedbackDTO dto = new TaskFeedbackDTO();
            dto.setActualDosage(actualDosage);
            dto.setActualArea(actualArea);
            dto.setFeedbackText(feedbackText);
            dto.setFeedbackImages(feedbackImages);
            dto.setResourceUsage(resourceUsage);
            return dto;
        }
    }

    public static class AssignRequest {
        private Integer executorId;
        private String status;

        public Integer getExecutorId() {
            return executorId;
        }

        public void setExecutorId(Integer executorId) {
            this.executorId = executorId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public static class StatusRequest {
        private String status;
        private String resourceUsage;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getResourceUsage() {
            return resourceUsage;
        }

        public void setResourceUsage(String resourceUsage) {
            this.resourceUsage = resourceUsage;
        }
    }
}
