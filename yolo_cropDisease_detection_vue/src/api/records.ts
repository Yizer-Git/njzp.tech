import request from '/@/utils/request';

/**
 * Disease mapping result from backend
 */
export interface DiseaseMappingResult {
	diseaseId: number;
	diseaseCode: string;
	diseaseName: string;
	cropId?: number;
	cropName?: string;
	originalLabel: string;
	riskLevel?: string;
	isHealthy: boolean;
}

/**
 * Get disease mapping for an image record
 * @param recordId image record ID
 * @returns disease mapping results
 */
export const getImgRecordDiseaseMapping = (recordId: number) => {
	return request({
		url: `/api/imgRecords/${recordId}/disease-mapping`,
		method: 'get',
	});
};

/**
 * Get disease mapping for a video record
 * @param recordId video record ID
 * @param label detected disease label
 * @returns disease mapping results
 */
export const getVideoRecordDiseaseMapping = (recordId: number, label: string) => {
	return request({
		url: `/api/videoRecords/${recordId}/disease-mapping`,
		method: 'get',
		params: { label },
	});
};

/**
 * Get disease mapping for a camera record
 * @param recordId camera record ID
 * @param label detected disease label
 * @returns disease mapping results
 */
export const getCameraRecordDiseaseMapping = (recordId: number, label: string) => {
	return request({
		url: `/api/cameraRecords/${recordId}/disease-mapping`,
		method: 'get',
		params: { label },
	});
};

