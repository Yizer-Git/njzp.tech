import request from '/@/utils/request';

export interface TaskCreationPayload {
	fieldId?: number | null;
	taskType?: string | null;
	planStartTime?: string | null;
	planEndTime?: string | null;
	executorId?: number | null;
	description?: string | null;
	resourceUsage?: string | null;
}

export interface CreateFromSolutionPayload {
	solutionId: number;
	recordId?: number | null;
	task: TaskCreationPayload;
}

export interface TaskFeedbackPayload {
	actualDosage?: number | null;
	actualArea?: number | null;
	feedbackText?: string | null;
	feedbackImages?: string[];
	resourceUsage?: string | null;
}

export const fetchTaskPage = (params: Record<string, any>) => {
	return request({
		url: '/api/tasks',
		method: 'get',
		params,
	});
};

export const fetchTaskStats = () => {
	return request({
		url: '/api/tasks/stats',
		method: 'get',
	});
};

export const fetchChainStats = (params?: Record<string, any>) => {
	return request({
		url: '/api/tasks/chain-stats',
		method: 'get',
		params,
	});
};

export const createTask = (data: Record<string, any>) => {
	return request({
		url: '/api/tasks',
		method: 'post',
		data,
	});
};

export const createFromSolution = (data: CreateFromSolutionPayload) => {
	return request({
		url: '/api/tasks/createFromSolution',
		method: 'post',
		data,
	});
};

export const updateTask = (id: number, data: Record<string, any>) => {
	return request({
		url: `/api/tasks/${id}`,
		method: 'put',
		data,
	});
};

export const assignTask = (id: number, data: Record<string, any>) => {
	return request({
		url: `/api/tasks/${id}/assign`,
		method: 'put',
		data,
	});
};

export const updateTaskStatus = (id: number, status: string, resourceUsage?: string) => {
	return request({
		url: `/api/tasks/${id}/status`,
		method: 'put',
		data: { status, resourceUsage },
	});
};

export const submitTaskFeedback = (id: number, data: TaskFeedbackPayload) => {
	return request({
		url: `/api/tasks/${id}/feedback`,
		method: 'post',
		data,
	});
};

export const archiveTask = (id: number) => {
	return request({
		url: `/api/tasks/${id}/archive`,
		method: 'put',
	});
};

export const fetchTaskChain = (id: number) => {
	return request({
		url: `/api/tasks/${id}/chain`,
		method: 'get',
	});
};

export const removeTask = (id: number) => {
	return request({
		url: `/api/tasks/${id}`,
		method: 'delete',
	});
};
