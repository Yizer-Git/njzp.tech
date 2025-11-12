<template>
	<div class="device-overview-card">
		<div class="device-overview-card__header">
			<div class="device-overview-card__title">
				<el-icon class="device-overview-card__title-icon">
					<Cpu />
				</el-icon>
				<div class="device-overview-card__title-text">
					<span class="title-main">
						{{ summary?.deviceName || selectedDeviceId || '未选择设备' }}
					</span>
					<span class="title-sub">设备概览</span>
				</div>
				<el-tag
					class="device-overview-card__status-tag"
					:type="summary?.online ? 'success' : 'danger'"
					effect="dark"
					size="large"
				>
					{{ statusText }}
				</el-tag>
			</div>
			<div class="device-overview-card__actions">
				<el-select
					v-model="innerSelectedDeviceId"
					placeholder="请选择设备"
					class="device-overview-card__device-select"
					clearable
					:teleported="false"
				>
					<el-option v-for="device in devices" :key="device" :label="device" :value="device" />
				</el-select>
				<el-button type="primary" :loading="refreshing" plain @click="emit('refresh')">
					<el-icon>
						<Refresh />
					</el-icon>
					刷新
				</el-button>
			</div>
		</div>

		<el-skeleton :rows="2" :loading="loading" animated class="device-overview-card__skeleton" />

		<div v-if="!loading" class="device-overview-card__meta">
			<div class="device-overview-card__meta-item">
				<el-icon><Connection /></el-icon>
				<span class="label">信号强度</span>
				<span class="value">
					{{ displaySignal }}
				</span>
			</div>
			<div class="device-overview-card__meta-item">
				<el-icon><AlarmClock /></el-icon>
				<span class="label">上次心跳</span>
				<el-tooltip v-if="heartbeatTooltip" :content="heartbeatTooltip" placement="top">
					<span class="value value--underline">{{ heartbeatText }}</span>
				</el-tooltip>
				<span v-else class="value">
					{{ heartbeatText }}
				</span>
			</div>
				<div class="device-overview-card__meta-item">
					<el-icon><Lightning /></el-icon>
				<span class="label">电量状态</span>
				<span class="value">{{ batteryText }}</span>
			</div>
		</div>

		<div v-if="!loading" class="device-overview-card__metrics">
			<div v-for="metric in metrics" :key="metric.key" class="device-overview-card__metric">
				<div class="metric-icon" :style="{ backgroundColor: iconBackground }">
					<el-icon v-if="metric.icon">
						<component :is="metric.icon" />
					</el-icon>
				</div>
				<div class="metric-content">
					<span class="metric-label">{{ metric.label }}</span>
					<span class="metric-value">
						{{ metric.value }}
						<span v-if="metric.unit" class="metric-unit">{{ metric.unit }}</span>
					</span>
				</div>
				<el-tag v-if="metric.badgeType" :type="metric.badgeType" size="small" effect="plain" round>
					{{ trendText(metric.trend) }}
				</el-tag>
			</div>
		</div>

		<el-empty v-if="!loading && !metrics.length" class="device-overview-card__empty" description="暂无实时指标" />
	</div>
</template>

<script lang="ts" setup>
import { computed, ref, watch } from 'vue';
import { AlarmClock, Connection, Cpu, Lightning, Refresh } from '@element-plus/icons-vue';
import { formatDate, formatPast } from '/@/utils/formatTime';
import type { DeviceStatusSummary, OverviewMetric } from '../types';

const props = defineProps<{
	devices: string[];
	selectedDeviceId: string;
	summary: DeviceStatusSummary | null;
	metrics: OverviewMetric[];
	loading: boolean;
	refreshing?: boolean;
}>();

const emit = defineEmits<{
	(e: 'refresh'): void;
	(e: 'select', deviceId: string | null): void;
}>();

const innerSelectedDeviceId = ref(props.selectedDeviceId);

watch(
	() => props.selectedDeviceId,
	(val) => {
		innerSelectedDeviceId.value = val;
	}
);

watch(innerSelectedDeviceId, (val) => {
	emit('select', val || null);
});

const statusText = computed(() => {
	if (!props.summary) return '未连接';
	if (props.summary.statusText) return props.summary.statusText;
	return props.summary.online ? '在线' : '离线';
});

const displaySignal = computed(() => {
	const signal = props.summary?.signalStrength;
	if (signal === null || signal === undefined) {
		return '未上报';
	}
	const parsed = Number(signal);
	if (Number.isNaN(parsed)) return `${signal}`;
	if (parsed <= 0) return '--';
	return `${parsed}%`;
});

const batteryText = computed(() => {
	const battery = props.summary?.batteryLevel;
	if (battery === null || battery === undefined) {
		return '--';
	}
	const parsed = Number(battery);
	if (Number.isNaN(parsed)) return `${battery}`;
	return `${parsed}%`;
});

const heartbeatDate = computed(() => {
	if (!props.summary?.lastHeartbeat) return null;
	const date = new Date(props.summary.lastHeartbeat);
	if (Number.isNaN(date.getTime())) return null;
	return date;
});

const heartbeatText = computed(() => {
	if (!heartbeatDate.value) return '暂无心跳数据';
	return formatPast(heartbeatDate.value, 'YYYY-mm-dd HH:MM:SS');
});

const heartbeatTooltip = computed(() => {
	if (!heartbeatDate.value) return '';
	return formatDate(heartbeatDate.value, 'YYYY-mm-dd HH:MM:SS');
});

const iconBackground = '#f0f8f5';

const trendText = (trend?: OverviewMetric['trend']) => {
	if (!trend) return '实时';
	switch (trend) {
		case 'up':
			return '上升';
		case 'down':
			return '下降';
		default:
			return '稳定';
	}
};
</script>

<style scoped lang="scss">
.device-overview-card {
	display: flex;
	flex-direction: column;
	gap: 16px;
	padding: 20px;
	border-radius: 16px;
	background: linear-gradient(135deg, rgba(40, 167, 69, 0.08), rgba(76, 175, 80, 0.02));
	box-shadow: 0 12px 32px rgba(18, 106, 59, 0.12);

	&__header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 16px;
	}

	&__title {
		display: flex;
		align-items: center;
		gap: 12px;
	}

	&__title-icon {
		font-size: 28px;
		color: #2f855a;
	}

	&__title-text {
		display: flex;
		flex-direction: column;
		gap: 4px;

		.title-main {
			font-size: 20px;
			font-weight: 600;
			color: #1c3d2b;
			line-height: 1;
		}

		.title-sub {
			font-size: 12px;
			color: rgba(28, 61, 43, 0.6);
		}
	}

	&__status-tag {
		border-radius: 999px;
	}

	&__actions {
		display: flex;
		align-items: center;
		gap: 12px;
	}

	&__device-select {
		width: 220px;
	}

	&__meta {
		display: grid;
		grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
		gap: 12px;
		padding: 12px 16px;
		border-radius: 12px;
		background: rgba(255, 255, 255, 0.72);
		backdrop-filter: blur(4px);

		&-item {
			display: flex;
			align-items: center;
			gap: 8px;
			color: #1c3d2b;

			.label {
				font-size: 12px;
				opacity: 0.7;
			}

			.value {
				font-size: 14px;
				font-weight: 600;

				&--underline {
					cursor: help;
					text-decoration: underline dotted rgba(28, 61, 43, 0.4);
					text-underline-offset: 3px;
				}
			}
		}
	}

	&__metrics {
		display: grid;
		grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
		gap: 16px;
	}

	&__metric {
		display: flex;
		align-items: center;
		gap: 14px;
		padding: 14px 16px;
		border-radius: 14px;
		background: #ffffff;
		box-shadow: 0 12px 24px rgba(18, 106, 59, 0.08);

		.metric-icon {
			display: flex;
			align-items: center;
			justify-content: center;
			width: 40px;
			height: 40px;
			border-radius: 12px;
			color: #2f855a;
			background-color: rgba(47, 133, 90, 0.12);
		}

		.metric-content {
			display: flex;
			flex-direction: column;
			gap: 2px;
		}

		.metric-label {
			font-size: 13px;
			color: rgba(27, 47, 37, 0.7);
		}

		.metric-value {
			font-size: 18px;
			font-weight: 600;
			color: #1c3d2b;
		}

		.metric-unit {
			margin-left: 4px;
			font-size: 12px;
			color: rgba(27, 47, 37, 0.5);
		}
	}

	&__skeleton {
		margin-top: -12px;
	}

	&__empty {
		padding: 30px 0;
	}
}

@media (max-width: 768px) {
	.device-overview-card {
		padding: 16px;

		&__header {
			flex-direction: column;
			align-items: flex-start;
		}

		&__actions {
			width: 100%;

			:deep(.el-select) {
				flex: 1;
			}
		}

		&__device-select {
			width: 100%;
		}
	}
}
</style>
