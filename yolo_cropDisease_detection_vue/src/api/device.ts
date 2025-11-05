import request from '/@/utils/request';

export interface DeviceExecutePayload {
	taskId: number;
	deviceId: string;
	command?: string;
	parameters?: Record<string, any>;
}

export const executeDeviceControl = (data: DeviceExecutePayload) => {
	return request({
		url: '/api/device/execute',
		method: 'post',
		data,
	});
};

