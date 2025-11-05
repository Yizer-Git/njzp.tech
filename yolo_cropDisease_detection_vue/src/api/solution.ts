import request from '/@/utils/request';

export interface SolutionOption {
	solutionId: number;
	diseaseId: number;
	diseaseName: string;
	diseaseRiskLevel?: string;
	cropId?: number;
	cropName?: string;
	remedyId: number;
	remedyName?: string;
	recommendedDosage?: number;
	dosageUnit?: string;
}

export interface GenerateSolutionPayload {
	diseaseId: number;
	cropId?: number;
	latitude?: number;
	longitude?: number;
}

export const fetchSolutionCatalog = () => {
	return request({
		url: '/api/solution/catalog',
		method: 'get',
	});
};

export const generateSolutionPlan = (data: GenerateSolutionPayload) => {
	return request({
		url: '/api/solution/generate',
		method: 'post',
		data,
	});
};

export const fetchWeatherSnapshot = (params?: { latitude?: number; longitude?: number }) => {
	return request({
		url: '/api/weather/current',
		method: 'get',
		params,
	});
};
