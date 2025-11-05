package com.example.Kcsj.service;

import com.example.Kcsj.dto.TaskChainDTO;
import com.example.Kcsj.dto.TaskCreationDTO;
import com.example.Kcsj.dto.TaskFeedbackDTO;
import com.example.Kcsj.entity.TaskEntity;

public interface TaskService {

    /**
     * Create a new task derived from a solution and optional recognition record.
     *
     * @param solutionId solution identifier
     * @param recordId   image/video recognition record identifier, may be null
     * @param payload    task creation payload
     * @return created task id
     */
    Long createFromSolution(Long solutionId, Long recordId, TaskCreationDTO payload);

    /**
     * Submit execution feedback for a task.
     */
    void submitFeedback(Long taskId, TaskFeedbackDTO feedback);

    /**
     * Archive a completed task.
     */
    void archiveTask(Long taskId);

    /**
     * Retrieve the end-to-end chain data for a task.
     */
    TaskChainDTO getTaskChain(Long taskId);

    /**
     * Update task status with state-machine validation.
     */
    TaskEntity updateTaskStatus(Long taskId, String newStatus);
}

