<template>
	<div class="device-control-page">
		<DeviceOverviewBar
			:devices="deviceOptions"
			:selected-device-id="selectedDeviceId"
			:summary="summary"
			:metrics="overviewMetrics"
			:loading="overviewLoading"
			:refreshing="snapshotLoading || overviewLoading"
			@select="handleDeviceSelect"
			@refresh="refreshAll"
		/>

		<div class="device-control-page__grid">
			<div class="grid-main">
				<RealTimeMonitor
					:trend-data="trendData"
					:snapshot="snapshot"
					:loading="trendLoading || snapshotLoading"
					:range="trendRange"
					@update:range="handleTrendRangeChange"
					@refresh="refreshRealtime"
				/>

				<SmartLinkagePanel
					:scenes="sceneList"
					:conditions="conditionTips"
					:loading="snapshotLoading"
					:disabled="!hasDeviceSelected || !!actionLoading"
					@trigger="executeScene"
				/>
			</div>

			<div class="grid-side">
				<div class="control-module-grid">
					<ControlModuleCard
						title="智慧灌溉"
						subtitle="继电器 1"
						description="稳定土壤湿度，避免过干或过湿"
						action-type="switch"
						:icon="HotWater"
						:model-value="relayStates.RELAY_WATER"
						:disabled="!hasDeviceSelected"
						:loading="actionLoading === 'RELAY_WATER'"
						:status-type="relayStates.RELAY_WATER ? 'success' : 'info'"
						:status-text="relayStates.RELAY_WATER ? '灌溉中' : '待命'"
						:last-result="executionMap.RELAY_WATER || null"
						@update:modelValue="(value: boolean) => handleRelayChange('RELAY_WATER', value)"
					/>

					<ControlModuleCard
						title="循环通风"
						subtitle="继电器 2"
						description="快速带走热量与湿气，保持温室舒适"
						action-type="switch"
						:icon="WindPower"
						:model-value="relayStates.RELAY_FAN"
						:disabled="!hasDeviceSelected"
						:loading="actionLoading === 'RELAY_FAN'"
						:status-type="relayStates.RELAY_FAN ? 'success' : 'info'"
						:status-text="relayStates.RELAY_FAN ? '运行中' : '待命'"
						:last-result="executionMap.RELAY_FAN || null"
						@update:modelValue="(value: boolean) => handleRelayChange('RELAY_FAN', value)"
					/>

					<ControlModuleCard
						title="声光告警"
						subtitle="蜂鸣器"
						description="异常事件快速提醒工作人员"
						action-type="trigger"
						:icon="BellFilled"
						:disabled="!hasDeviceSelected"
						:loading="actionLoading === 'BUZZER'"
						trigger-label="触发蜂鸣器"
						:last-result="executionMap.BUZZER || null"
						@trigger="triggerBuzzer"
					/>
				</div>
			</div>
		</div>
	</div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import {
	BellFilled,
	ColdDrink,
	HotWater,
	Lightning,
	MagicStick,
	MostlyCloudy,
	Moon,
	Sunny,
	WindPower,
} from '@element-plus/icons-vue';
import DeviceOverviewBar from './components/DeviceOverviewBar.vue';
import ControlModuleCard from './components/ControlModuleCard.vue';
import RealTimeMonitor from './components/RealTimeMonitor.vue';
import SmartLinkagePanel from './components/SmartLinkagePanel.vue';
import type {
	ConditionTip,
	DeviceStatusSummary,
	OverviewMetric,
	SceneConfig,
	SensorSnapshot,
	SensorTrendGranularity,
	SensorTrendPoint,
} from './types';
import { fetchLatestSensorData, fetchSensorDevices, fetchSensorSummary, fetchSensorTrend } from '/@/api/sensor';
import { controlDevice } from '/@/api/device';

const deviceOptions = ref<string[]>([]);
const selectedDeviceId = ref<string>('');

const overviewLoading = ref(false);
const summary = ref<DeviceStatusSummary | null>(null);

const snapshot = ref<SensorSnapshot | null>(null);
const snapshotLoading = ref(false);

const trendData = ref<SensorTrendPoint[]>([]);
const trendRange = ref<SensorTrendGranularity>('1h');
const trendLoading = ref(false);

const actionLoading = ref('');
const relayStates = reactive<Record<'RELAY_WATER' | 'RELAY_FAN', boolean>>({
	RELAY_WATER: false,
	RELAY_FAN: false,
});
const executionMap = reactive<Record<string, string>>({});

const sceneList = ref<SceneConfig[]>([
	{
		id: 'cooling',
		name: '智慧降温',
		description: '开启风扇并触发微喷，快速降低温室温度',
		icon: ColdDrink,
		highlightColor: '#4fd1c5',
		actions: [
			{ action: 'RELAY_FAN', value: true, deviceLabel: '循环风扇', durationSeconds: 300 },
			{ action: 'RELAY_WATER', value: true, deviceLabel: '雾化喷淋', durationSeconds: 120 },
		],
	},
	{
		id: 'night-care',
		name: '夜间守护',
		description: '自动检测夜间湿度，必要时短暂开启灌溉',
		icon: Moon,
		highlightColor: '#7f9cf5',
		actions: [{ action: 'RELAY_WATER', value: true, deviceLabel: '灌溉泵', durationSeconds: 90 }],
	},
	{
		id: 'emergency',
		name: '异常预警',
		description: '触发蜂鸣器并记录事件，提醒值守人员',
		icon: Lightning,
		highlightColor: '#f6ad55',
		actions: [{ action: 'BUZZER', value: true, deviceLabel: '蜂鸣器', durationSeconds: 5 }],
	},
	{
		id: 'smart-care',
		name: '智慧养护',
		description: '温湿双控策略即将上线，敬请期待',
		icon: MagicStick,
		highlightColor: '#c084fc',
		actions: [],
		comingSoon: true,
	},
]);

const hasDeviceSelected = computed(() => !!selectedDeviceId.value);

const overviewMetrics = computed<OverviewMetric[]>(() => {
	const data = snapshot.value;
	if (!data) return [];
	const metrics: OverviewMetric[] = [];

	const pushMetric = (
		key: string,
		label: string,
		value: number | string | null | undefined,
		unit = '',
		icon?: OverviewMetric['icon'],
		badgeType?: OverviewMetric['badgeType'],
	) => {
		let display = '--';
		if (value !== null && value !== undefined) {
			if (typeof value === 'number' && Number.isFinite(value)) {
				display = value % 1 === 0 ? value.toString() : value.toFixed(1);
			} else {
				display = String(value);
			}
		}
		metrics.push({
			key,
			label,
			value: display,
			unit: display === '--' ? '' : unit,
			icon,
			badgeType,
		});
	};

	const formatWaterLevel = (level?: number | null) => {
		if (level === null || level === undefined) return null;
		const map: Record<number, string> = {
			0: '正常',
			1: '缺水',
			2: '水满',
		};
		return map[Number(level)] || `状态 ${level}`;
	};

	pushMetric(
		'airTemperature',
		'空气温度',
		data.airTemperature,
		'℃',
		Sunny,
		typeof data.airTemperature === 'number' && data.airTemperature > 35 ? 'danger' : undefined
	);
	pushMetric('airHumidity', '空气湿度', data.airHumidity, '%', MostlyCloudy);
	pushMetric(
		'soilHumidity',
		'土壤湿度',
		data.soilHumidity,
		'%',
		HotWater,
		typeof data.soilHumidity === 'number' && data.soilHumidity < 30 ? 'warning' : undefined
	);
	pushMetric(
		'lightIntensity',
		'光照强度',
		typeof data.lightIntensity === 'number' ? Number(data.lightIntensity / 1000).toFixed(1) : null,
		'klux',
		MagicStick
	);
	pushMetric('waterLevel', '水位状态', formatWaterLevel(data.waterLevel), '', ColdDrink, data.waterLevel === 1 ? 'warning' : undefined);

	return metrics;
});

const conditionTips = computed<ConditionTip[]>(() => {
	const tips: ConditionTip[] = [];
	const data = snapshot.value;
	if (!data) return tips;

	if (typeof data.soilHumidity === 'number' && data.soilHumidity < 30) {
		tips.push({
			id: 'soil-low',
			level: 'warning',
			message: '土壤湿度偏低，建议开启灌溉泵或使用联动方案。',
			suggestion: '执行“智慧灌溉”或“智慧降温”场景',
			metricKey: 'soilHumidity',
			metricValue: data.soilHumidity,
			metricUnit: '%',
		});
	}

	if (typeof data.airTemperature === 'number' && data.airTemperature > 35) {
		tips.push({
			id: 'temp-high',
			level: 'danger',
			message: '温室温度过高，植株可能出现热害。',
			suggestion: '立即开启循环风扇，必要时执行“智慧降温”场景',
			metricKey: 'airTemperature',
			metricValue: data.airTemperature,
			metricUnit: '℃',
		});
	}

	if (data.waterLevel === 1) {
		tips.push({
			id: 'water-low',
			level: 'warning',
			message: '水箱水位偏低，请及时补水以免影响灌溉任务。',
			metricKey: 'waterLevel',
			metricValue: data.waterLevel,
		});
	}

	return tips;
});

const loadDevices = async () => {
	try {
		const res = await fetchSensorDevices();
		if (res.code === 0 && Array.isArray(res.data)) {
			deviceOptions.value = res.data;
			if (!selectedDeviceId.value && res.data.length) {
				selectedDeviceId.value = res.data[0];
			}
		}
	} catch (error) {
		console.error('加载设备列表失败', error);
		ElMessage.error('加载设备列表失败');
	}
};

const loadSummary = async () => {
	if (!selectedDeviceId.value) return;
	overviewLoading.value = true;
	try {
		const res = await fetchSensorSummary(selectedDeviceId.value);
		if (res.code === 0) {
			summary.value = res.data || null;
		} else {
			summary.value = null;
		}
	} catch (error) {
		console.error('获取设备状态失败', error);
		summary.value = null;
	} finally {
		overviewLoading.value = false;
	}
};

const loadSnapshot = async () => {
	if (!selectedDeviceId.value) {
		snapshot.value = null;
		return;
	}
	snapshotLoading.value = true;
	try {
		const res = await fetchLatestSensorData(selectedDeviceId.value);
		if (res.code === 0) {
			snapshot.value = res.data || null;
		} else {
			snapshot.value = null;
		}
	} catch (error) {
		console.error('获取实时传感器数据失败', error);
		snapshot.value = null;
	} finally {
		snapshotLoading.value = false;
	}
};

const loadTrend = async () => {
	if (!selectedDeviceId.value) {
		trendData.value = [];
		return;
	}
	trendLoading.value = true;
	try {
		const res = await fetchSensorTrend({
			deviceId: selectedDeviceId.value,
			range: trendRange.value,
		});
		if (res.code === 0 && Array.isArray(res.data)) {
			trendData.value = res.data;
		} else {
			trendData.value = [];
		}
	} catch (error) {
		console.error('获取传感器趋势数据失败', error);
		trendData.value = [];
	} finally {
		trendLoading.value = false;
	}
};

const refreshAll = async () => {
	if (!selectedDeviceId.value) return;
	await Promise.all([loadSummary(), loadSnapshot(), loadTrend()]);
};

const refreshRealtime = async () => {
	if (!selectedDeviceId.value) return;
	await Promise.all([loadSnapshot(), loadTrend()]);
};

const handleDeviceSelect = (deviceId: string | null) => {
	selectedDeviceId.value = deviceId || '';
};

const handleTrendRangeChange = (range: SensorTrendGranularity) => {
	trendRange.value = range;
};

const sendControlCommand = async (action: string, value: any) => {
	if (!selectedDeviceId.value) {
		ElMessage.warning('请先选择设备');
		return false;
	}
	try {
		actionLoading.value = action;
		const res = await controlDevice({
			deviceId: selectedDeviceId.value,
			action,
			value,
		});
		if (res.code === 0) {
			const label = typeof value === 'boolean' ? (value ? '开启' : '关闭') : '执行';
			executionMap[action] = `${new Date().toISOString()}|${label}`;
			ElMessage.success('指令已发送');
			return true;
		}
		throw new Error(res.msg || '指令发送失败');
	} catch (error) {
		console.error('发送控制指令失败', error);
		const message = error instanceof Error ? error.message : '发送指令失败';
		ElMessage.error(message);
		return false;
	} finally {
		actionLoading.value = '';
	}
};

const handleRelayChange = async (action: 'RELAY_WATER' | 'RELAY_FAN', value: boolean) => {
	const prev = relayStates[action];
	relayStates[action] = value;
	const succeed = await sendControlCommand(action, value);
	if (!succeed) {
		relayStates[action] = prev;
	}
};

const triggerBuzzer = async () => {
	await sendControlCommand('BUZZER', true);
};

const executeScene = async (scene: SceneConfig) => {
	if (scene.comingSoon) return;
	if (!selectedDeviceId.value) {
		ElMessage.warning('请先选择设备');
		return;
	}
	for (const action of scene.actions) {
		const succeed = await sendControlCommand(action.action, action.value ?? true);
		if (!succeed) break;
	}
};

watch(selectedDeviceId, (value) => {
	if (value) {
		refreshAll();
	} else {
		summary.value = null;
		snapshot.value = null;
		trendData.value = [];
	}
});

watch(trendRange, (range) => {
	if (selectedDeviceId.value) {
		loadTrend();
	}
});

onMounted(async () => {
	await loadDevices();
	if (selectedDeviceId.value) {
		refreshAll();
	}
});
</script>

<style scoped lang="scss">
.device-control-page {
	padding: 20px;

	&__grid {
		display: grid;
		grid-template-columns: minmax(0, 7fr) minmax(280px, 4fr);
		gap: 20px;
		margin-top: 20px;
	}
}

.grid-main {
	display: flex;
	flex-direction: column;
	gap: 20px;
}

.grid-side {
	display: flex;
	flex-direction: column;
	gap: 20px;
}

.control-module-grid {
	display: grid;
	grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
	gap: 18px;
}

@media (max-width: 1200px) {
	.device-control-page__grid {
		grid-template-columns: 1fr;
	}
}
</style>
