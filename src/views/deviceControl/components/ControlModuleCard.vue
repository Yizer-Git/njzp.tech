<template>
	<div class="control-module-card">
		<div class="control-module-card__header">
			<div class="control-module-card__title">
				<div class="control-module-card__icon">
					<el-icon>
						<component :is="icon" />
					</el-icon>
				</div>
				<div class="control-module-card__title-text">
					<span class="title-main">{{ title }}</span>
					<span v-if="subtitle" class="title-sub">{{ subtitle }}</span>
				</div>
			</div>
			<div class="control-module-card__status">
				<span class="status-lamp" :class="`status-lamp--${statusType}`" />
				<span class="status-text">{{ statusText }}</span>
			</div>
		</div>

		<p v-if="description" class="control-module-card__description">{{ description }}</p>

		<div class="control-module-card__body">
			<template v-if="actionType === 'switch'">
				<el-switch
					:model-value="modelValue"
					:disabled="disabled"
					:loading="loading"
					active-text="开启"
					inactive-text="关闭"
					inline-prompt
					@change="handleSwitch"
				/>
			</template>
			<template v-else>
				<el-button type="danger" :disabled="disabled" :loading="loading" plain size="large" @click="emit('trigger')">
					<el-icon class="pulse-icon" v-if="loading">
						<Loading />
					</el-icon>
					{{ triggerLabel }}
				</el-button>
			</template>
		</div>

		<div class="control-module-card__footer">
			<div class="footer-item">
				<span class="label">最近执行</span>
				<span class="value">{{ lastResultText }}</span>
			</div>
			<div class="footer-item" v-if="countdown !== null">
				<span class="label">预计结束</span>
				<span class="value value--countdown">{{ countdownDisplay }}</span>
			</div>
		</div>
	</div>
</template>

<script lang="ts" setup>
import { computed } from 'vue';
import type { Component } from 'vue';
import { Loading } from '@element-plus/icons-vue';
import { formatPast } from '/@/utils/formatTime';

const props = defineProps<{
	title: string;
	icon: Component;
	actionType: 'switch' | 'trigger';
	loading: boolean;
	description?: string;
	subtitle?: string;
	modelValue?: boolean;
	disabled?: boolean;
	statusType?: 'success' | 'warning' | 'danger' | 'info';
	statusText?: string;
	countdown?: number | null;
	lastResult?: string | null;
	triggerLabel?: string;
}>();

const emit = defineEmits<{
	(e: 'update:modelValue', value: boolean): void;
	(e: 'trigger'): void;
}>();

const statusType = computed(() => props.statusType || (props.modelValue ? 'success' : 'info'));

const statusText = computed(() => {
	if (props.statusText) return props.statusText;
	if (props.actionType === 'switch') {
		return props.modelValue ? '运行中' : '待命';
	}
	return props.loading ? '执行中' : '待触发';
});

const countdownDisplay = computed(() => {
	if (props.countdown === null || props.countdown === undefined) return '--';
	if (props.countdown <= 0) return '即将结束';
	const minutes = Math.floor(props.countdown / 60);
	const seconds = props.countdown % 60;
	return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
});

const lastResultText = computed(() => {
	if (!props.lastResult) return '暂无执行记录';
	const [timestamp, ...rest] = props.lastResult.split('|');
	try {
		const date = new Date(timestamp);
		if (!Number.isNaN(date.getTime())) {
			return `${formatPast(date, 'HH:MM')} · ${rest.join('|').trim()}`;
		}
	} catch {
		//
	}
	return props.lastResult;
});

const triggerLabel = computed(() => props.triggerLabel || '立即触发');

const handleSwitch = (value: boolean) => {
	emit('update:modelValue', value);
};
</script>

<style scoped lang="scss">
$card-shadow: 0 18px 32px rgba(23, 122, 71, 0.12);
$border-radius: 18px;
$lamp-success: #2ecc71;
$lamp-warning: #f9a825;
$lamp-danger: #e53935;
$lamp-info: #1e88e5;

.control-module-card {
	display: flex;
	flex-direction: column;
	gap: 12px;
	padding: 18px;
	border-radius: $border-radius;
	background: #ffffff;
	box-shadow: $card-shadow;
	transition: transform 0.25s ease, box-shadow 0.25s ease;

	&:hover {
		transform: translateY(-4px);
		box-shadow: 0 20px 40px rgba(23, 122, 71, 0.14);
	}

	&__header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 12px;
	}

	&__title {
		display: flex;
		align-items: center;
		gap: 12px;
	}

	&__icon {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 44px;
		height: 44px;
		border-radius: 16px;
		background: rgba(47, 133, 90, 0.12);
		color: #2f855a;
		font-size: 22px;
	}

	&__title-text {
		display: flex;
		flex-direction: column;
		gap: 2px;

		.title-main {
			font-size: 16px;
			font-weight: 600;
			color: #1f4035;
		}

		.title-sub {
			font-size: 12px;
			color: rgba(31, 64, 53, 0.6);
		}
	}

	&__status {
		display: flex;
		align-items: center;
		gap: 8px;

		.status-text {
			font-size: 12px;
			color: rgba(31, 64, 53, 0.75);
		}
	}

	&__description {
		margin: 0;
		font-size: 13px;
		color: rgba(31, 64, 53, 0.64);
	}

	&__body {
		display: flex;
		align-items: center;
		justify-content: center;
		min-height: 76px;
	}

	&__footer {
		display: grid;
		grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
		gap: 8px;
		padding-top: 8px;
		border-top: 1px dashed rgba(47, 133, 90, 0.24);

		.footer-item {
			display: flex;
			flex-direction: column;
			gap: 2px;
			font-size: 12px;
			color: rgba(31, 64, 53, 0.7);

			.value {
				font-weight: 600;
				color: #1f4035;
			}

			.value--countdown {
				font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
				font-size: 14px;
			}
		}
	}
}

.status-lamp {
	position: relative;
	width: 10px;
	height: 10px;
	border-radius: 50%;
	background: $lamp-info;
	box-shadow: 0 0 8px rgba(30, 136, 229, 0.6);
	display: inline-flex;

	&::after {
		content: '';
		position: absolute;
		inset: -6px;
		border-radius: 50%;
		opacity: 0.3;
		animation: pulse 1.6s infinite;
	}

	&--success {
		background: $lamp-success;
		box-shadow: 0 0 8px rgba($lamp-success, 0.7);

		&::after {
			background: rgba($lamp-success, 0.35);
		}
	}

	&--warning {
		background: $lamp-warning;
		box-shadow: 0 0 8px rgba($lamp-warning, 0.6);

		&::after {
			background: rgba($lamp-warning, 0.35);
		}
	}

	&--danger {
		background: $lamp-danger;
		box-shadow: 0 0 8px rgba($lamp-danger, 0.6);

		&::after {
			background: rgba($lamp-danger, 0.35);
		}
	}

	&--info {
		background: $lamp-info;
		box-shadow: 0 0 8px rgba($lamp-info, 0.6);

		&::after {
			background: rgba($lamp-info, 0.35);
		}
	}
}

@keyframes pulse {
	0% {
		transform: scale(0.8);
		opacity: 0.4;
	}
	50% {
		transform: scale(1);
		opacity: 0.8;
	}
	100% {
		transform: scale(0.8);
		opacity: 0.4;
	}
}
</style>
