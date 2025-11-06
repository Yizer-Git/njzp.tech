<template>
	<div class="smart-linkage">
		<div class="smart-linkage__header">
			<div class="smart-linkage__title">
				<el-icon class="smart-linkage__title-icon">
					<Link />
				</el-icon>
				<div class="smart-linkage__title-text">
					<span class="main">智能联动</span>
					<span class="sub">一键触发多设备协同</span>
				</div>
			</div>
			<el-tag v-if="comingSoonCount" effect="dark" type="warning" round>部分场景即将上线</el-tag>
		</div>

		<el-skeleton :loading="loading" animated :count="2" :rows="3" />

		<div v-if="!loading" class="smart-linkage__content">
			<div class="smart-linkage__scenes">
				<div v-for="scene in scenes" :key="scene.id" class="scene-card" :style="sceneStyle(scene)">
					<div class="scene-card__header">
						<div class="scene-card__icon">
							<el-icon>
								<component :is="scene.icon" />
							</el-icon>
						</div>
						<el-tag v-if="scene.comingSoon" size="small" effect="plain" type="info">即将上线</el-tag>
					</div>
					<div class="scene-card__title">{{ scene.name }}</div>
					<p class="scene-card__desc">{{ scene.description }}</p>
					<ul class="scene-card__actions">
						<li v-for="action in scene.actions" :key="`${scene.id}-${action.action}`">
							{{ action.deviceLabel || '设备' }} · {{ action.action }}
							<span v-if="action.durationSeconds"> ({{ action.durationSeconds }}s) </span>
						</li>
					</ul>
					<el-button
						size="small"
						type="primary"
						:disabled="scene.comingSoon || disabled"
						@click="emit('trigger', scene)"
					>
						{{ scene.comingSoon ? '敬请期待' : '立即执行' }}
					</el-button>
				</div>
			</div>

			<div class="smart-linkage__conditions">
				<h4 class="conditions-title">状态提示</h4>
				<div v-if="!conditions.length" class="conditions-empty">
					<el-empty description="当前指标正常" />
				</div>
				<div v-else class="conditions-list">
					<div v-for="condition in conditions" :key="condition.id" class="condition-card" :class="`condition-card--${condition.level}`">
						<div class="condition-card__header">
							<el-tag :type="tagType(condition.level)" effect="dark" size="small">
								{{ levelLabel(condition.level) }}
							</el-tag>
							<span class="condition-card__metric" v-if="condition.metricKey && condition.metricValue !== undefined">
								当前值：{{ condition.metricValue }}
								<span v-if="condition.metricUnit">{{ condition.metricUnit }}</span>
							</span>
						</div>
						<p class="condition-card__message">{{ condition.message }}</p>
						<p v-if="condition.suggestion" class="condition-card__suggestion">
							建议：{{ condition.suggestion }}
						</p>
					</div>
				</div>
			</div>
		</div>
	</div>
</template>

<script lang="ts" setup>
import { computed } from 'vue';
import type { ConditionTip, SceneConfig } from '../types';
import { Link } from '@element-plus/icons-vue';

const props = defineProps<{
	scenes: SceneConfig[];
	conditions: ConditionTip[];
	loading: boolean;
	disabled?: boolean;
}>();

const emit = defineEmits<{
	(e: 'trigger', scene: SceneConfig): void;
}>();

const comingSoonCount = computed(() => props.scenes.filter((scene) => scene.comingSoon).length);

const sceneStyle = (scene: SceneConfig) => ({
	borderColor: scene.highlightColor,
	boxShadow: `0 12px 24px ${scene.highlightColor}33`,
});

const levelLabel = (level: ConditionTip['level']) => {
	switch (level) {
		case 'danger':
			return '紧急';
		case 'warning':
			return '提醒';
		default:
			return '提示';
	}
};

const tagType = (level: ConditionTip['level']) => {
	switch (level) {
		case 'danger':
			return 'danger';
		case 'warning':
			return 'warning';
		default:
			return 'info';
	}
};
</script>

<style scoped lang="scss">
.smart-linkage {
	display: flex;
	flex-direction: column;
	gap: 18px;
	padding: 18px;
	border-radius: 18px;
	background: #ffffff;
	box-shadow: 0 20px 36px rgba(18, 87, 62, 0.12);

	&__header {
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	&__title {
		display: flex;
		align-items: center;
		gap: 12px;
	}

	&__title-icon {
		font-size: 22px;
		color: #2f855a;
	}

	&__title-text {
		display: flex;
		flex-direction: column;
		gap: 2px;

		.main {
			font-size: 18px;
			font-weight: 600;
			color: #1f4035;
		}

		.sub {
			font-size: 12px;
			color: rgba(31, 64, 53, 0.65);
		}
	}

	&__content {
		display: grid;
		grid-template-columns: 1fr 320px;
		gap: 18px;
	}

	&__scenes {
		display: grid;
		grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
		gap: 16px;
	}

	&__conditions {
		display: flex;
		flex-direction: column;
		gap: 12px;
		padding: 16px;
		border-radius: 16px;
		background: rgba(47, 133, 90, 0.08);
	}
}

.scene-card {
	display: flex;
	flex-direction: column;
	gap: 12px;
	padding: 16px;
	border-radius: 16px;
	border: 1px solid rgba(47, 133, 90, 0.2);
	background: #ffffff;
	transition: transform 0.25s ease, box-shadow 0.25s ease;

	&:hover {
		transform: translateY(-4px);
		box-shadow: 0 22px 40px rgba(47, 133, 90, 0.16);
	}

	&__header {
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	&__icon {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 42px;
		height: 42px;
		border-radius: 14px;
		background: rgba(47, 133, 90, 0.12);
		color: #2f855a;
		font-size: 22px;
	}

	&__title {
		font-size: 16px;
		font-weight: 600;
		color: #1f4035;
	}

	&__desc {
		margin: 0;
		color: rgba(31, 64, 53, 0.65);
		font-size: 13px;
		line-height: 1.4;
		min-height: 36px;
	}

	&__actions {
		margin: 0;
		padding-left: 18px;
		color: rgba(31, 64, 53, 0.7);
		font-size: 12px;
		line-height: 1.6;
	}
}

.conditions-title {
	margin: 0;
	font-size: 14px;
	font-weight: 600;
	color: #1f4035;
}

.conditions-list {
	display: flex;
	flex-direction: column;
	gap: 12px;
}

.conditions-empty {
	flex: 1;
	display: flex;
	align-items: center;
	justify-content: center;
}

.condition-card {
	padding: 12px 14px;
	border-radius: 14px;
	background: #ffffff;
	box-shadow: 0 10px 24px rgba(47, 133, 90, 0.12);
	display: flex;
	flex-direction: column;
	gap: 8px;

	&__header {
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	&__metric {
		font-size: 12px;
		color: rgba(31, 64, 53, 0.7);
	}

	&__message {
		margin: 0;
		font-size: 13px;
		color: #1f4035;
		font-weight: 600;
	}

	&__suggestion {
		margin: 0;
		font-size: 12px;
		color: rgba(31, 64, 53, 0.7);
	}

	&--danger {
		border: 1px solid rgba(229, 57, 53, 0.25);
	}

	&--warning {
		border: 1px solid rgba(251, 188, 5, 0.35);
	}

	&--info {
		border: 1px solid rgba(33, 150, 243, 0.25);
	}
}

@media (max-width: 1024px) {
	.smart-linkage__content {
		grid-template-columns: 1fr;
	}
}
</style>
