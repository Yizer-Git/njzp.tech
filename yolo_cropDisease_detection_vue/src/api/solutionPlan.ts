import request from '/@/utils/request';

/**
 * Solution plan entity
 */
export interface SolutionPlan {
	id?: number;
	diseaseId: number;
	cropId?: number;
	cropName?: string;
	remedyId: number;
	recommendedDosage: number;
	dosageUnit: string;
	applicationStage?: string;
	applicationTiming?: string;
	notes?: string;
	weatherConstraints?: string;
	expectedCost?: number;
	status: string;
	createdAt?: string;
	updatedAt?: string;
}

/**
 * Get all solution plans
 */
export const getAllSolutionPlans = () => {
	return request({
		url: '/api/solution-plan/all',
		method: 'get',
	});
};

/**
 * Get solution plan by ID
 */
export const getSolutionPlanById = (id: number) => {
	return request({
		url: `/api/solution-plan/${id}`,
		method: 'get',
	});
};

/**
 * Get solution plans with pagination
 */
export const getSolutionPlanList = (params: {
	pageNum?: number;
	pageSize?: number;
	diseaseId?: number;
	cropId?: number;
	cropName?: string;
	status?: string;
}) => {
	return request({
		url: '/api/solution-plan/list',
		method: 'get',
		params,
	});
};

/**
 * Create new solution plan
 */
export const createSolutionPlan = (data: SolutionPlan) => {
	return request({
		url: '/api/solution-plan',
		method: 'post',
		data,
	});
};

/**
 * Update solution plan
 */
export const updateSolutionPlan = (id: number, data: SolutionPlan) => {
	return request({
		url: `/api/solution-plan/${id}`,
		method: 'put',
		data,
	});
};

/**
 * Update solution plan status
 */
export const updateSolutionPlanStatus = (id: number, status: string) => {
	return request({
		url: `/api/solution-plan/${id}/status`,
		method: 'put',
		params: { status },
	});
};

/**
 * Delete solution plan
 */
export const deleteSolutionPlan = (id: number) => {
	return request({
		url: `/api/solution-plan/${id}`,
		method: 'delete',
	});
};

