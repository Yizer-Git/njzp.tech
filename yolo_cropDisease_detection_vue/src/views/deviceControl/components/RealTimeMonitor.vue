<template>
	<div class="real-time-monitor">
		<div class="real-time-monitor__header">
			<div class="real-time-monitor__title">
				<el-icon class="real-time-monitor__title-icon">
					<DataLine />
				</el-icon>
				<div class="real-time-monitor__title-text">
					<span class="main">实时监测</span>
					<span class="sub">环境数据快照与趋势</span>
				</div>
			</div>

			<div class="real-time-monitor__actions">
				<el-radio-group v-model="innerRange" size="small">
					<el-radio-button label="1h">近1小时</el-radio-button>
					<el-radio-button label="6h">近6小时</el-radio-button>
				</el-radio-group>
				<el-button size="small" :loading="loading" @click="emit('refresh')">
					<el-icon><Refresh /></el-icon>
					刷新
				</el-button>
			</div>
		</div>

		<el-skeleton :loading="loading && !trendData.length" animated :rows="4" class="real-time-monitor__skeleton" />

		<div v-if="!loading" class="real-time-monitor__content">
			<div class="real-time-monitor__snapshot">
				<div class="snapshot-card" v-for="metric in snapshotMetrics" :key="metric.key">
					<div class="snapshot-card__meta">
						<span class="label">{{ metric.label }}</span>
						<el-tag v-if="metric.badge" :type="metric.badge" size="small" effect="plain">
							{{ metric.badgeText }}
						</el-tag>
					</div>
					<div class="snapshot-card__value">
						{{ metric.display }}
						<span v-if="metric.unit" class="unit">{{ metric.unit }}</span>
					</div>
					<el-progress
						v-if="metric.progress !== null"
						type="circle"
						:percentage="metric.progress"
						:width="92"
						:stroke-width="8"
					/>
				</div>
			</div>

			<div class="real-time-monitor__chart">
				<VChart v-if="chartOption" :option="chartOption" autoresize class="chart-canvas" />
				<el-empty
					v-else
					description="暂无趋势数据"
					image-size="120"
					class="real-time-monitor__empty"
				>
					<el-button type="primary" size="small" @click="emit('refresh')">尝试刷新</el-button>
				</el-empty>
			</div>
		</div>
	</div>
</template>

<script lang="ts" setup>
import { computed, watch, ref } from 'vue';
import type { SensorSnapshot, SensorTrendGranularity, SensorTrendPoint } from '../types';
import { DataLine, Refresh } from '@element-plus/icons-vue';
import VChart from 'vue-echarts';
import { use } from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import { LineChart } from 'echarts/charts';
import { DatasetComponent, GridComponent, LegendComponent, TooltipComponent } from 'echarts/components';
import type { EChartsOption } from 'echarts';
import { formatDate } from '/@/utils/formatTime';

use([CanvasRenderer, LineChart, TooltipComponent, LegendComponent, GridComponent, DatasetComponent]);

const props = defineProps<{
	trendData: SensorTrendPoint[];
	snapshot: SensorSnapshot | null;
	loading: boolean;
	range: SensorTrendGranularity;
}>();

const emit = defineEmits<{
	(e: 'update:range', value: SensorTrendGranularity): void;
	(e: 'refresh'): void;
}>();

const innerRange = ref<SensorTrendGranularity>(props.range);

watch(
	() => props.range,
	(val) => {
		innerRange.value = val;
	}
);

watch(innerRange, (val) => {
	emit('update:range', val);
});

const snapshotMetrics = computed(() => {
	if (!props.snapshot) {
		return [
			{ key: 'empty', label: '暂无实时数据', display: '--', unit: '', progress: null, badge: '', badgeText: '' },
		];
	}
	const { soilHumidity, airHumidity, airTemperature, lightIntensity, waterLevel } = props.snapshot;

	const soilProgress = soilHumidity === null || soilHumidity === undefined ? null : Math.min(100, Math.max(0, Math.round(soilHumidity)));
	const airHumidityProgress =
		airHumidity === null || airHumidity === undefined ? null : Math.min(100, Math.max(0, Math.round(airHumidity)));

	const waterLabelMap: Record<number, string> = {
		0: '正常',
		1: '缺水',
		2: '水满',
	};

	return [
		{
			key: 'soil',
			label: '土壤湿度',
			display: soilHumidity !== null && soilHumidity !== undefined ? soilHumidity.toFixed(0) : '--',
			unit: soilHumidity !== null && soilHumidity !== undefined ? '%' : '',
			progress: soilProgress,
			badge: soilHumidity !== null && soilHumidity < 30 ? 'warning' : '',
			badgeText: soilHumidity !== null && soilHumidity < 30 ? '偏低' : soilHumidity !== null ? '正常' : '',
		},
		{
			key: 'airHumidity',
			label: '空气湿度',
			display: airHumidity !== null && airHumidity !== undefined ? airHumidity.toFixed(0) : '--',
			unit: airHumidity !== null && airHumidity !== undefined ? '%' : '',
			progress: airHumidityProgress,
			badge: '',
			badgeText: '',
		},
		{
			key: 'temperature',
			label: '空气温度',
			display: airTemperature !== null && airTemperature !== undefined ? airTemperature.toFixed(1) : '--',
			unit: airTemperature !== null && airTemperature !== undefined ? '℃' : '',
			progress: null,
			badge: airTemperature !== null && airTemperature > 35 ? 'danger' : '',
			badgeText: airTemperature !== null && airTemperature > 35 ? '偏高' : '',
		},
		{
			key: 'light',
			label: '光照强度',
			display: lightIntensity !== null && lightIntensity !== undefined ? (lightIntensity / 1000).toFixed(1) : '--',
			unit: lightIntensity !== null && lightIntensity !== undefined ? 'klux' : '',
			progress: null,
			badge: '',
			badgeText: '',
		},
		{
			key: 'water',
			label: '水位状态',
			display:
				waterLevel !== null && waterLevel !== undefined
					? waterLabelMap[Number(waterLevel)] || `状态 ${waterLevel}`
					: '--',
			unit: '',
			progress: null,
			badge: waterLevel === 1 ? 'warning' : waterLevel === 2 ? 'info' : '',
			badgeText: waterLevel === 1 ? '需补水' : waterLevel === 2 ? '水满' : '',
		},
	];
});

const chartOption = computed<EChartsOption | null>(() => {
	if (!props.trendData.length) return null;
	const timestamps = props.trendData.map((item) => {
		const date = new Date(item.timestamp);
		if (Number.isNaN(date.getTime())) return item.timestamp;
		return formatDate(date, 'HH:MM');
	});

	const temperatureSeries = props.trendData.map((item) =>
		item.airTemperature === null || item.airTemperature === undefined ? null : Number(item.airTemperature)
	);
	const soilSeries = props.trendData.map((item) =>
		item.soilHumidity === null || item.soilHumidity === undefined ? null : Number(item.soilHumidity)
	);
	const humiditySeries = props.trendData.map((item) =>
		item.airHumidity === null || item.airHumidity === undefined ? null : Number(item.airHumidity)
	);

	return {
		color: ['#2f855a', '#00a676', '#5ec2ff'],
		tooltip: {
			trigger: 'axis',
		},
		legend: {
			data: ['空气温度(℃)', '土壤湿度(%)', '空气湿度(%)'],
		},
		grid: {
			left: 30,
			right: 20,
			top: 50,
			bottom: 30,
		},
		dataset: {
			source: timestamps.map((time, index) => ({
				time,
				temp: temperatureSeries[index],
				soil: soilSeries[index],
				humidity: humiditySeries[index],
			})),
		},
		xAxis: {
			type: 'category',
		},
		yAxis: {
			type: 'value',
			min: (value: { min: number }) => Math.floor(value.min - 5),
		},
		series: [
			{
				type: 'line',
				name: '空气温度(℃)',
				smooth: true,
				encode: { x: 'time', y: 'temp' },
			},
			{
				type: 'line',
				name: '土壤湿度(%)',
				smooth: true,
				encode: { x: 'time', y: 'soil' },
			},
			{
				type: 'line',
				name: '空气湿度(%)',
				smooth: true,
				encode: { x: 'time', y: 'humidity' },
			},
		],
	};
});
</script>

<style scoped lang="scss">
.real-time-monitor {
	display: flex;
	flex-direction: column;
	gap: 18px;
	padding: 18px;
	border-radius: 18px;
	background: #ffffff;
	box-shadow: 0 20px 36px rgba(17, 97, 59, 0.12);

	&__header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 12px;
	}

	&__title {
		display: flex;
		align-items: center;
		gap: 10px;
	}

	&__title-icon {
		color: #2f855a;
		font-size: 22px;
	}

	&__title-text {
		display: flex;
		flex-direction: column;
		gap: 4px;

		.main {
			font-size: 18px;
			font-weight: 600;
			color: #1f4035;
		}

		.sub {
			font-size: 12px;
			color: rgba(31, 64, 53, 0.6);
		}
	}

	&__actions {
		display: flex;
		align-items: center;
		gap: 10px;
	}

	&__content {
		display: grid;
		grid-template-columns: 320px 1fr;
		gap: 18px;
	}

	&__snapshot {
		display: flex;
		flex-direction: column;
		gap: 14px;
	}

	&__chart {
		position: relative;
		height: 320px;
		background: linear-gradient(180deg, rgba(230, 244, 235, 0.5) 0%, rgba(255, 255, 255, 0.9) 100%);
		border-radius: 16px;
		padding: 12px;
	}

	&__empty {
		padding-top: 60px;
	}

	&__skeleton {
		margin-top: -8px;
	}
}

.snapshot-card {
	display: flex;
	flex-direction: column;
	gap: 12px;
	padding: 18px;
	border-radius: 16px;
	background: rgba(47, 133, 90, 0.08);
	backdrop-filter: blur(6px);

	&__meta {
		display: flex;
		align-items: center;
		justify-content: space-between;
		font-size: 13px;
		color: rgba(31, 64, 53, 0.72);
	}

	&__value {
		font-size: 26px;
		font-weight: 600;
		color: #1f4035;

		.unit {
			margin-left: 4px;
			font-size: 12px;
			color: rgba(31, 64, 53, 0.6);
		}
	}

	:deep(.el-progress__text) {
		color: #1f4035;
		font-weight: 600;
	}
}

.chart-canvas {
	height: 100%;
	width: 100%;
}

@media (max-width: 1024px) {
	.real-time-monitor__content {
		grid-template-columns: 1fr;
	}

	.real-time-monitor__chart {
		height: 280px;
	}
}
</style>
