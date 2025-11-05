import request from '/@/utils/request';
import type { DiseaseInfo } from './disease';

export interface EncyclopediaOverview {
	totalDiseases: number;
	cropCount: number;
	highRiskCount: number;
	mediumRiskCount: number;
	lowRiskCount: number;
}

export interface CropDiseaseGroup {
	cropName: string;
	cropDisplayName: string;
	diseaseCount: number;
	diseases: DiseaseInfo[];
}

export interface DiseaseEncyclopediaPayload {
	overview: EncyclopediaOverview;
	crops: CropDiseaseGroup[];
	pathogenTypes: string[];
	riskLevels: string[];
}

/**
 * Fetch aggregated disease encyclopedia data.
 */
export const getEncyclopediaData = () => {
	return request<DiseaseEncyclopediaPayload>({
		url: '/api/disease/encyclopedia',
		method: 'get',
	});
};

/**
 * Search diseases by keyword for quick lookup.
 */
export const searchDiseases = (keyword: string) => {
	return request<DiseaseInfo[]>({
		url: '/api/disease/encyclopedia/search',
		method: 'get',
		params: { keyword },
	});
};

