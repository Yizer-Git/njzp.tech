import request from '/@/utils/request';

/**
 * Remedy entity
 */
export interface Remedy {
	id?: number;
	remedyCode: string;
	remedyName: string;
	activeIngredient?: string;
	targetPathogen?: string;
	formulation?: string;
	safeDosage: number;
	dosageUnit: string;
	intervalDays: number;
	applicationMethod?: string;
	safetyIntervalDays?: number;
	maxApplicationsPerSeason?: number;
	caution?: string;
	costPerUnit: number;
	currency: string;
	lastPriceUpdate?: string;
	createdAt?: string;
	updatedAt?: string;
}

/**
 * Get all remedies
 */
export const getAllRemedies = () => {
	return request({
		url: '/api/remedy/all',
		method: 'get',
	});
};

/**
 * Get remedy by ID
 */
export const getRemedyById = (id: number) => {
	return request({
		url: `/api/remedy/${id}`,
		method: 'get',
	});
};

/**
 * Get remedies with pagination
 */
export const getRemedyList = (params: {
	pageNum?: number;
	pageSize?: number;
	remedyName?: string;
	activeIngredient?: string;
	targetPathogen?: string;
}) => {
	return request({
		url: '/api/remedy/list',
		method: 'get',
		params,
	});
};

/**
 * Create new remedy
 */
export const createRemedy = (data: Remedy) => {
	return request({
		url: '/api/remedy',
		method: 'post',
		data,
	});
};

/**
 * Update remedy
 */
export const updateRemedy = (id: number, data: Remedy) => {
	return request({
		url: `/api/remedy/${id}`,
		method: 'put',
		data,
	});
};

/**
 * Update remedy price
 */
export const updateRemedyPrice = (id: number, price: number) => {
	return request({
		url: `/api/remedy/${id}/price`,
		method: 'put',
		params: { price },
	});
};

/**
 * Delete remedy
 */
export const deleteRemedy = (id: number) => {
	return request({
		url: `/api/remedy/${id}`,
		method: 'delete',
	});
};

