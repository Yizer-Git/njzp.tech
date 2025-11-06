import request from '/@/utils/request';

export interface DeviceExecutePayload {
	taskId: number;
	deviceId: string;
	command?: string;
	parameters?: Record<string, any>;
}

export interface ControlRequest {
	deviceId: string;
	action: string;
	value?: any;
	taskId?: number;
}

export const executeDeviceControl = (data: DeviceExecutePayload) => {
	return request({
		url: '/api/device/execute',
		method: 'post',
		data,
	});
};

export const controlDevice = (data: ControlRequest) => {
	return request({
		url: '/api/device/control',
		method: 'post',
		data,
	});
};
