import request from '/@/utils/request';

export const fetchSensorDevices = () => {
	return request({
		url: '/api/sensor/devices',
		method: 'get',
	});
};

export const fetchLatestSensorData = (deviceId?: string) => {
	return request({
		url: '/api/sensor/latest',
		method: 'get',
		params: deviceId ? { deviceId } : undefined,
	});
};

export const fetchSensorHistory = (params: {
	deviceId?: string;
	startTime?: string;
	endTime?: string;
}) => {
	return request({
		url: '/api/sensor/history',
		method: 'get',
		params,
	});
};

export const fetchSensorSummary = (deviceId?: string) => {
	return request({
		url: '/api/sensor/summary',
		method: 'get',
		params: deviceId ? { deviceId } : undefined,
	});
};

export const fetchSensorTrend = (params: {
	deviceId?: string;
	range?: string;
}) => {
	return request({
		url: '/api/sensor/trend',
		method: 'get',
		params,
	});
};
