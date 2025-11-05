import request from '/@/utils/request';

/**
 * Disease information entity
 */
export interface DiseaseInfo {
	id?: number;
	diseaseCode: string;
	diseaseName: string;
	cropId?: number;
	cropName?: string;
	description?: string;
	symptomSummary?: string;
	pathogenType?: string;
	riskLevel?: string;
	climateRiskFactors?: string;
	createdAt?: string;
	updatedAt?: string;
}

/**
 * Get all diseases
 */
export const getAllDiseases = () => {
	return request({
		url: '/api/disease/all',
		method: 'get',
	});
};

/**
 * Get disease by ID
 */
export const getDiseaseById = (id: number) => {
	return request({
		url: `/api/disease/${id}`,
		method: 'get',
	});
};

/**
 * Get diseases with pagination
 */
export const getDiseaseList = (params: {
	pageNum?: number;
	pageSize?: number;
	diseaseName?: string;
	cropName?: string;
	riskLevel?: string;
}) => {
	return request({
		url: '/api/disease/list',
		method: 'get',
		params,
	});
};

/**
 * Create new disease
 */
export const createDisease = (data: DiseaseInfo) => {
	return request({
		url: '/api/disease',
		method: 'post',
		data,
	});
};

/**
 * Update disease
 */
export const updateDisease = (id: number, data: DiseaseInfo) => {
	return request({
		url: `/api/disease/${id}`,
		method: 'put',
		data,
	});
};

/**
 * Delete disease
 */
export const deleteDisease = (id: number) => {
	return request({
		url: `/api/disease/${id}`,
		method: 'delete',
	});
};

