export interface DeviceStatusSummary {
	deviceId: string;
	deviceName?: string;
	online: boolean;
	signalStrength?: number | null;
	lastHeartbeat?: string | null;
	batteryLevel?: number | null;
	statusText?: string;
	extra?: Record<string, any>;
}

import type { Component } from 'vue';

export interface OverviewMetric {
	key: string;
	label: string;
	value: string;
	unit?: string;
	rawValue?: number | string | null;
	icon?: Component;
	badgeType?: 'success' | 'warning' | 'info' | 'danger';
	trend?: 'up' | 'down' | 'stable';
}

export type SensorMetricKey =
	| 'airTemperature'
	| 'airHumidity'
	| 'soilHumidity'
	| 'lightIntensity'
	| 'waterLevel'
	| 'co2Level'
	| 'batteryLevel';

export type SensorTrendGranularity = '1h' | '6h';

export type SensorSnapshot = Partial<Record<SensorMetricKey, number | null>> & {
	deviceId?: string;
	timestamp?: string | null;
	raw?: Record<string, any>;
};

export interface SensorTrendPoint {
	timestamp: string;
	airTemperature?: number | null;
	airHumidity?: number | null;
	soilHumidity?: number | null;
	lightIntensity?: number | null;
	waterLevel?: number | null;
	[key: string]: number | string | null | undefined;
}

export interface SceneAction {
	action: string;
	value?: any;
	deviceLabel?: string;
	durationSeconds?: number;
}

export interface SceneConfig {
	id: string;
	name: string;
	description: string;
	icon: Component;
	highlightColor: string;
	actions: SceneAction[];
	comingSoon?: boolean;
}

export interface ConditionTip {
	id: string;
	level: 'info' | 'warning' | 'danger';
	message: string;
	suggestion?: string;
	metricKey?: SensorMetricKey;
	metricValue?: number | null;
	metricUnit?: string;
}

export interface ExecutionRecord {
	id: string;
	deviceId: string;
	action: string;
	operator?: string;
	status?: 'success' | 'failed' | 'pending' | 'in_progress';
	createdAt?: string;
	resultMessage?: string;
	durationSeconds?: number | null;
	source?: string;
}
