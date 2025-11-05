<template>
	<div class="solution-page">
		<el-card shadow="hover" class="banner-card">
			<div class="banner-content">
				<div class="banner-text">
					<h2>智能防治方案中心</h2>
					<p>融合知识库、实时气象与处方建议的作业决策中枢</p>
				</div>
				<el-button type="primary" plain :icon="RefreshRight" @click="refreshAll" :loading="state.loadingAll">
					一键刷新
				</el-button>
			</div>
		</el-card>

		<el-card shadow="hover" class="section-card parameter-card">
			<div class="section-header">
				<el-icon class="section-icon">
					<Collection />
				</el-icon>
				<div>
					<div class="section-title-text">方案参数</div>
					<div class="section-subtitle">横向设置核心参数，快速生成定制方案</div>
				</div>
			</div>
			<el-skeleton :loading="state.catalogLoading" animated :rows="3">
				<div class="parameter-content">
					<div class="parameter-field">
						<div class="field-label">病害-作物-药剂组合</div>
						<el-select
							v-model="state.selectedSolutionId"
							placeholder="请选择方案"
							filterable
							@change="autoGenerate"
						>
							<el-option
								v-for="item in state.catalog"
								:key="item.solutionId"
								:label="formatOptionLabel(item)"
								:value="item.solutionId"
							/>
						</el-select>
					</div>
					<div class="parameter-field">
						<div class="field-label with-icon">
							<el-icon><LocationInformation /></el-icon>
							<span>作业坐标 (纬度 / 经度)</span>
						</div>
						<div class="coord-inputs">
							<el-input-number
								v-model="state.latitude"
								:step="0.01"
								:min="-90"
								:max="90"
								controls-position="right"
							/>
							<el-input-number
								v-model="state.longitude"
								:step="0.01"
								:min="-180"
								:max="180"
								controls-position="right"
							/>
						</div>
					</div>
					<div class="parameter-actions">
						<div class="field-label">操作</div>
						<div class="action-buttons">
							<el-button type="primary" :loading="state.solutionLoading" @click="handleGenerate">
								生成方案
							</el-button>
							<el-button @click="resetDefaults" :disabled="state.solutionLoading || state.catalogLoading">
								恢复默认
							</el-button>
						</div>
					</div>
				</div>
			</el-skeleton>
		</el-card>

		<el-card shadow="hover" class="section-card recommendation-card">
			<div class="section-header">
				<el-icon class="section-icon">
					<DocumentChecked />
				</el-icon>
				<div>
					<div class="section-title-text">推荐方案</div>
					<div class="section-subtitle">固定布局呈现施药要点、风险提示与成本估算</div>
				</div>
				<div
					v-if="state.recommendation"
					:class="riskBadgeClass(state.recommendation?.diseaseRiskLevel)"
					class="risk-badge"
				>
					{{ state.recommendation?.diseaseRiskLevel || '风险未评估' }}
				</div>
			</div>
			<el-skeleton :loading="state.solutionLoading && !state.recommendation" animated :rows="6">
				<div v-if="state.recommendation" class="recommendation-content">
					<div class="rec-overview">
						<div class="overview-item">
							<div class="overview-label">目标病害</div>
							<div class="overview-value line-clamp">
								{{ state.recommendation.diseaseName || '未识别' }}
							</div>
						</div>
						<div class="overview-item">
							<div class="overview-label">目标作物</div>
							<div class="overview-value line-clamp">
								{{ state.recommendation.cropName || '未指定' }}
							</div>
						</div>
						<div class="overview-item">
							<div class="overview-label">推荐药剂</div>
							<div class="overview-value line-clamp">
								{{ state.recommendation.remedyName }}
								<span class="overview-subvalue">{{ state.recommendation.activeIngredient }}</span>
							</div>
						</div>
						<div class="overview-item" v-if="state.recommendation.generatedAt">
							<div class="overview-label">生成时间</div>
							<div class="overview-value">
								{{ formatTime(state.recommendation.generatedAt) }}
							</div>
						</div>
					</div>

					<div class="rec-grid">
						<div class="grid-card">
							<div class="grid-title">
								<el-icon><Timer /></el-icon>
								<span>施药要点</span>
							</div>
							<ul class="decorated-list">
								<li>
									推荐剂量：{{ formatDosage(state.recommendation.recommendedDosage, state.recommendation.dosageUnit) }}
								</li>
								<li>施药阶段：{{ state.recommendation.applicationStage || '依据作物长势调整' }}</li>
								<li>施药时机：{{ state.recommendation.applicationTiming || '关注病害中心区域' }}</li>
							</ul>
						</div>
						<div class="grid-card">
							<div class="grid-title warning">
								<el-icon><WarningFilled /></el-icon>
								<span>风险提示</span>
							</div>
							<ul class="decorated-list">
								<li
									v-for="(item, index) in state.recommendation.riskWarnings"
									:key="index"
									class="line-clamp"
								>
									{{ item }}
								</li>
								<li v-if="!state.recommendation.riskWarnings || !state.recommendation.riskWarnings.length">
									暂无特别风险提示
								</li>
							</ul>
						</div>
						<div class="grid-card">
							<div class="grid-title">
								<el-icon><TrendCharts /></el-icon>
								<span>成本估算</span>
							</div>
							<div class="cost-highlight">
								<span v-if="state.recommendation.computedCost !== null" class="amount">
									{{ formatCurrency(state.recommendation.computedCost, state.recommendation.currency) }}
								</span>
								<span v-else class="amount muted">--</span>
								<span class="unit">预计投入</span>
							</div>
							<p class="cost-breakdown line-clamp">
								{{ state.recommendation.costBreakdown || '暂无成本拆解说明' }}
							</p>
						</div>
					</div>

					<div class="rec-footer">
						<div class="footer-block">
							<div class="footer-title">
								<el-icon><Calendar /></el-icon>
								<span>最佳时间窗口</span>
							</div>
							<div v-if="recommendationTimeWindows.length" class="tag-group">
								<el-tag
									v-for="(window, index) in recommendationTimeWindows"
									:key="index"
									type="success"
									effect="dark"
								>
									{{ window }}
								</el-tag>
							</div>
							<p v-else class="placeholder-text">暂无推荐时间窗口</p>
						</div>
						<div class="footer-block">
							<div class="footer-title warning">
								<el-icon><WarningFilled /></el-icon>
								<span>作业限制条件</span>
							</div>
							<ul v-if="restrictionEntries.length" class="decorated-list vertical">
								<li v-for="([factor, detail], index) in restrictionEntries" :key="index" class="line-clamp">
									<strong>{{ factor }}：</strong>{{ detail }}
								</li>
							</ul>
							<p v-else class="placeholder-text">暂无特殊限制条件</p>
						</div>
					</div>
				</div>
				<div v-else class="empty-holder">
					<el-empty description="请选择方案后即可查看智能推荐结果" />
				</div>
			</el-skeleton>
		</el-card>

		<el-card shadow="hover" class="section-card weather-card">
			<div class="section-header">
				<el-icon class="section-icon">
					<Sunny />
				</el-icon>
				<div>
					<div class="section-title-text">气象快照</div>
					<div class="section-subtitle">结合当前气象与未来趋势辅助作业安排</div>
				</div>
			</div>
			<el-skeleton :loading="state.weatherLoading && !state.weather" animated :rows="4">
				<div v-if="state.weather" class="weather-content">
					<div class="weather-top">
						<div class="snapshot-grid">
							<div class="snapshot-card">
								<div class="snapshot-label">
									<el-icon><TrendCharts /></el-icon>
									<span>当前温度</span>
								</div>
								<div class="snapshot-value">
									<span class="number">{{ formatNumber(state.weather.currentTemperature) }}</span>
									<span class="unit">℃</span>
								</div>
							</div>
							<div class="snapshot-card">
								<div class="snapshot-label">
									<el-icon><Histogram /></el-icon>
									<span>空气湿度</span>
								</div>
								<div class="snapshot-value">
									<span class="number">{{ formatPercentage(state.weather.currentHumidity) }}</span>
								</div>
							</div>
							<div class="snapshot-card">
								<div class="snapshot-label">
									<el-icon><Timer /></el-icon>
									<span>风速</span>
								</div>
								<div class="snapshot-value">
									<span class="number">{{ formatNumber(state.weather.windSpeed) }}</span>
									<span class="unit">m/s</span>
								</div>
							</div>
							<div class="snapshot-card">
								<div class="snapshot-label">
									<el-icon><WarningFilled /></el-icon>
									<span>降雨概率</span>
								</div>
								<div class="snapshot-value">
									<span class="number">{{ formatPercentage(state.weather.precipitationProbability) }}</span>
								</div>
							</div>
						</div>
						<el-descriptions :column="3" border size="small" class="meta-descriptions">
							<el-descriptions-item label="数据来源">{{ state.weather.source || '—' }}</el-descriptions-item>
							<el-descriptions-item label="更新时间">{{ formatTime(state.weather.fetchedAt) }}</el-descriptions-item>
							<el-descriptions-item label="天气摘要">
								<span class="line-clamp">{{ state.weather.weatherSummary || '暂无摘要' }}</span>
							</el-descriptions-item>
						</el-descriptions>
					</div>
					<div class="weather-chart-wrapper">
						<VueECharts
							v-if="weatherChartOptions"
							ref="chartRef"
							class="weather-chart"
							:option="weatherChartOptions"
							autoresize
						/>
						<el-empty v-else description="暂无有效的天气预报数据" />
					</div>
				</div>
				<div v-else class="empty-holder">
					<el-empty description="暂未获取到气象数据" />
				</div>
			</el-skeleton>
		</el-card>
	</div>
</template>

<script setup lang="ts" name="solutionPlan">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import {
	Calendar,
	Collection,
	DocumentChecked,
	Histogram,
	LocationInformation,
	RefreshRight,
	Sunny,
	Timer,
	TrendCharts,
	WarningFilled,
} from '@element-plus/icons-vue';
import { useRoute } from 'vue-router';
import type { SolutionOption } from '/@/api/solution';
import { fetchSolutionCatalog, fetchWeatherSnapshot, generateSolutionPlan } from '/@/api/solution';
import type { EChartsOption } from 'echarts';
import { VueECharts } from 'vue-echarts';
import { use } from 'echarts/core';
import { BarChart, LineChart } from 'echarts/charts';
import { CanvasRenderer } from 'echarts/renderers';
import { DataZoomComponent, GridComponent, LegendComponent, TooltipComponent } from 'echarts/components';

use([BarChart, LineChart, CanvasRenderer, GridComponent, TooltipComponent, LegendComponent, DataZoomComponent]);

interface ApiResponse<T> {
	code: string;
	msg: string;
	data: T;
}

interface WeatherForecastItem {
	date: string;
	temperatureMax?: number;
	temperatureMin?: number;
	precipitationSum?: number;
	sunshineDuration?: number;
}

interface WeatherSnapshot {
	currentTemperature?: number;
	currentHumidity?: number;
	windSpeed?: number;
	precipitationProbability?: number;
	precipitationAmount?: number;
	solarRadiation?: number;
	weatherSummary?: string;
	source?: string;
	fetchedAt?: string;
	forecast?: WeatherForecastItem[];
}

interface SolutionRecommendation {
	diseaseId: number;
	diseaseName: string;
	diseaseRiskLevel?: string;
	cropId?: number;
	cropName?: string;
	remedyId: number;
	remedyName: string;
	activeIngredient?: string;
	formulation?: string;
	intervalDays?: number;
	safetyIntervalDays?: number;
	maxApplicationsPerSeason?: number;
	recommendedDosage?: number;
	dosageUnit?: string;
	computedCost: number | null;
	currency?: string;
	costBreakdown?: string;
	applicationStage?: string;
	applicationTiming?: string;
	applicationNotes?: string[];
	riskWarnings?: string[];
	recommendedTimeWindows?: string[];
	applicationRestrictions?: Record<string, string>;
	weatherAdvisory?: string;
	generatedAt?: string;
}

const DEFAULT_LAT = 30.67;
const DEFAULT_LON = 104.06;

const state = reactive({
	catalog: [] as SolutionOption[],
	catalogLoading: false,
	solutionLoading: false,
	weatherLoading: false,
	loadingAll: false,
	selectedSolutionId: undefined as number | undefined,
	recommendation: null as SolutionRecommendation | null,
	weather: null as WeatherSnapshot | null,
	latitude: DEFAULT_LAT,
	longitude: DEFAULT_LON,
});

const chartRef = ref<InstanceType<typeof VueECharts> | null>(null);

const currentOption = computed(() => {
	return state.catalog.find((item) => item.solutionId === state.selectedSolutionId);
});

const recommendationTimeWindows = computed(() => {
	return state.recommendation?.recommendedTimeWindows || [];
});

const restrictionEntries = computed(() => {
	const restrictions = state.recommendation?.applicationRestrictions;
	if (!restrictions) {
		return [];
	}
	return Object.entries(restrictions);
});

const weatherChartOptions = computed<EChartsOption | null>(() => {
	const forecast = state.weather?.forecast;
	if (!forecast || !forecast.length) {
		return null;
}
	const categories = forecast.map((item) => formatDate(item.date));
	const maxTemps = forecast.map((item) => (item.temperatureMax ?? null));
	const minTemps = forecast.map((item) => (item.temperatureMin ?? null));
	const rainfall = forecast.map((item) => (item.precipitationSum ?? 0));
	const visibleCount = Math.min(forecast.length, 7);
	const endValue = forecast.length ? Math.round((visibleCount / forecast.length) * 100) : 100;

	return {
		tooltip: {
			trigger: 'axis',
		},
		legend: {
			data: ['最高温', '最低温', '降雨量'],
			textStyle: {
				color: '#2a5164',
			},
		},
		grid: {
			left: 40,
			right: 40,
			top: 60,
			bottom: 80,
		},
		xAxis: {
			type: 'category',
			data: categories,
			axisTick: {
				alignWithLabel: true,
			},
			axisLabel: {
				color: '#516b7a',
			},
		},
		yAxis: [
			{
				type: 'value',
				name: '温度(℃)',
				axisLabel: {
					color: '#516b7a',
				},
				splitLine: {
					lineStyle: {
						type: 'dashed',
						color: '#dce7f1',
					},
				},
			},
			{
				type: 'value',
				name: '降雨量(mm)',
				axisLabel: {
					color: '#516b7a',
				},
				splitLine: {
					show: false,
				},
			},
		],
		dataZoom: [
			{
				type: 'slider',
				height: 18,
				bottom: 20,
				start: 0,
				end: endValue,
				handleSize: '120%',
				borderColor: 'transparent',
				backgroundColor: '#e8f5e9',
				fillerColor: '#b2dfdb',
			},
			{
				type: 'inside',
				start: 0,
				end: endValue,
			},
		],
		series: [
			{
				name: '最高温',
				type: 'bar',
				data: maxTemps,
				barMaxWidth: 24,
				itemStyle: {
					color: '#ffb74d',
					borderRadius: [6, 6, 0, 0],
				},
				emphasis: {
					focus: 'series',
				},
			},
			{
				name: '最低温',
				type: 'bar',
				data: minTemps,
				barMaxWidth: 24,
				itemStyle: {
					color: '#4fc3f7',
					borderRadius: [6, 6, 0, 0],
				},
				emphasis: {
					focus: 'series',
				},
			},
			{
				name: '降雨量',
				type: 'line',
				yAxisIndex: 1,
				data: rainfall,
				smooth: true,
				itemStyle: {
					color: '#66bb6a',
				},
				lineStyle: {
					width: 3,
				},
				areaStyle: {
					opacity: 0.15,
					color: '#a5d6a7',
				},
			},
		],
	};
});

const riskBadgeClass = (level?: string) => {
	if (!level) return 'risk-badge--unknown';
	const upper = level.toUpperCase();
	if (upper.includes('高') || upper.includes('HIGH')) return 'risk-badge--high';
	if (upper.includes('中') || upper.includes('MED')) return 'risk-badge--medium';
	return 'risk-badge--low';
};

const formatOptionLabel = (item: SolutionOption) => {
	const disease = item.diseaseName || '未知病害';
	const crop = item.cropName || '作物';
	const remedy = item.remedyName || '药剂';
	return `${disease} · ${crop} · ${remedy}`;
};

const formatDosage = (value?: number, unit?: string) => {
	if (value === undefined || value === null) return '请按说明执行';
	return `${Number(value).toFixed(2)} ${unit || ''}`.trim();
};

const formatCurrency = (value: number | null, currency?: string) => {
	if (value === null || value === undefined) return '--';
	return `${currency || 'CNY'} ${value.toFixed(2)}`;
};

const formatNumber = (value?: number, unit?: string) => {
	if (value === undefined || value === null) return '--';
	return `${Number(value).toFixed(1)}${unit ? ` ${unit}` : ''}`;
};

const formatPercentage = (value?: number) => {
	if (value === undefined || value === null) return '--';
	return `${(value * 100).toFixed(0)}%`;
};

const formatTime = (value?: string) => {
	if (!value) return '--';
	return value.replace('T', ' ');
};

const formatDate = (value?: string) => {
	if (!value) return '--';
	return value;
};

const loadCatalog = async () => {
	state.catalogLoading = true;
	try {
		const res: ApiResponse<SolutionOption[]> = await fetchSolutionCatalog();
		if (res.code === '0') {
			state.catalog = res.data || [];
			if (!state.selectedSolutionId && state.catalog.length > 0) {
				state.selectedSolutionId = state.catalog[0].solutionId;
			}
		} else {
			ElMessage.error(res.msg || '方案目录获取失败');
		}
	} catch (error) {
		ElMessage.error('方案目录获取失败');
	} finally {
		state.catalogLoading = false;
	}
};

const loadWeather = async () => {
	state.weatherLoading = true;
	try {
		const res: ApiResponse<WeatherSnapshot> = await fetchWeatherSnapshot({
			latitude: state.latitude,
			longitude: state.longitude,
		});
		if (res.code === '0') {
			state.weather = res.data;
			nextTick(() => {
				chartRef.value?.resize();
			});
		} else {
			ElMessage.error(res.msg || '气象数据获取失败');
		}
	} catch (error) {
		ElMessage.error('气象数据获取失败');
	} finally {
		state.weatherLoading = false;
	}
};

const handleGenerate = async () => {
	if (!state.selectedSolutionId) {
		ElMessage.warning('请先选择方案');
		return;
	}
	const option = currentOption.value;
	if (!option) {
		ElMessage.warning('未找到对应方案，请刷新后重试');
		return;
	}
	state.solutionLoading = true;
	try {
		const res: ApiResponse<SolutionRecommendation> = await generateSolutionPlan({
			diseaseId: option.diseaseId,
			cropId: option.cropId,
			latitude: state.latitude,
			longitude: state.longitude,
		});
		if (res.code === '0') {
			state.recommendation = res.data;
		} else {
			ElMessage.error(res.msg || '推荐方案生成失败');
		}
	} catch (error) {
		ElMessage.error('推荐方案生成失败');
	} finally {
		state.solutionLoading = false;
	}
};

const autoGenerate = () => {
	if (state.recommendation) {
		handleGenerate();
	}
};

const refreshAll = async () => {
	state.loadingAll = true;
	await Promise.all([loadCatalog(), loadWeather()]);
	await handleGenerate();
	state.loadingAll = false;
};

const resetDefaults = () => {
	state.latitude = DEFAULT_LAT;
	state.longitude = DEFAULT_LON;
	if (state.catalog.length > 0) {
		state.selectedSolutionId = state.catalog[0].solutionId;
	}
	state.recommendation = null;
	state.weather = null;
};

const route = useRoute();

const handleResize = () => {
	chartRef.value?.resize();
};

watch(
	() => state.weather?.forecast,
	() => {
		nextTick(() => {
			chartRef.value?.resize();
		});
	},
	{ deep: true }
);

onMounted(async () => {
	window.addEventListener('resize', handleResize);
	await loadCatalog();
	await loadWeather();

	const diseaseId = route.query.diseaseId;
	if (diseaseId) {
		const matchedOption = state.catalog.find((opt) => opt.diseaseId === Number(diseaseId));
		if (matchedOption) {
			state.selectedSolutionId = matchedOption.solutionId;
			await handleGenerate();
			ElMessage.success('已根据识别结果自动生成防治方案');
		} else {
			ElMessage.warning('未找到对应的防治方案，请手动选择');
		}
	} else if (state.selectedSolutionId) {
		handleGenerate();
	}
});

onBeforeUnmount(() => {
	window.removeEventListener('resize', handleResize);
});
</script>

<style scoped lang="scss">
.solution-page {
	display: flex;
	flex-direction: column;
	gap: 16px;
	min-height: 100%;

	.banner-card {
		border: none;
		background: linear-gradient(135deg, #e8f5e9, #f1f8ff);

		.banner-content {
			display: flex;
			align-items: center;
			justify-content: space-between;
			gap: 24px;
		}

		.banner-text {
			h2 {
				margin: 0 0 6px;
				font-size: 24px;
				font-weight: 700;
				color: #2a5164;
			}

			p {
				margin: 0;
				color: #4f6b7a;
			}
		}
	}

	.section-card {
		border: none;
		background-color: #fff;
		box-shadow: 0 6px 20px rgba(126, 191, 160, 0.12);
	}

	.section-header {
		display: flex;
		align-items: center;
		gap: 12px;
		margin-bottom: 20px;

		.section-icon {
			width: 40px;
			height: 40px;
			border-radius: 12px;
			display: flex;
			align-items: center;
			justify-content: center;
			background: linear-gradient(135deg, rgba(102, 187, 106, 0.24), rgba(38, 166, 154, 0.24));
			color: #1b5e20;
		}

		.section-title-text {
			font-size: 18px;
			font-weight: 600;
			color: #1b5e20;
		}

		.section-subtitle {
			font-size: 13px;
			color: #5a7a86;
		}
	}

	.parameter-card {
		.parameter-content {
			display: grid;
			grid-template-columns: repeat(3, minmax(0, 1fr));
			gap: 20px;
			align-items: flex-end;
		}

		.field-label {
			font-size: 13px;
			font-weight: 600;
			color: #52727b;
			margin-bottom: 8px;
			display: flex;
			align-items: center;
			gap: 6px;

			&.with-icon {
				color: #1b5e20;
			}
		}

		.coord-inputs {
			display: grid;
			grid-template-columns: repeat(2, minmax(0, 1fr));
			gap: 12px;
		}

		.parameter-actions {
			display: flex;
			flex-direction: column;
			justify-content: flex-end;

			.action-buttons {
				display: flex;
				gap: 12px;
			}
		}
	}

	.recommendation-card {
		.risk-badge {
			padding: 6px 14px;
			border-radius: 999px;
			font-size: 13px;
			font-weight: 600;
			color: #fff;
			transition: transform 0.3s ease;

			&.risk-badge--high {
				background: linear-gradient(135deg, #f44336, #ff7043);
			}

			&.risk-badge--medium {
				background: linear-gradient(135deg, #ffb300, #ffca28);
				color: #3e2723;
			}

			&.risk-badge--low {
				background: linear-gradient(135deg, #66bb6a, #43a047);
			}

			&.risk-badge--unknown {
				background: linear-gradient(135deg, #90a4ae, #607d8b);
			}
		}

		.recommendation-content {
			display: flex;
			flex-direction: column;
			gap: 24px;
		}

		.rec-overview {
			display: grid;
			grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
			gap: 16px;
			padding: 14px;
			border-radius: 12px;
			background: linear-gradient(135deg, rgba(200, 230, 201, 0.4), rgba(225, 245, 254, 0.4));
		}

		.overview-item {
			display: flex;
			flex-direction: column;
			gap: 6px;
		}

		.overview-label {
			font-size: 12px;
			font-weight: 600;
			color: #1b5e20;
			text-transform: uppercase;
			letter-spacing: 0.04em;
		}

		.overview-value {
			font-size: 15px;
			font-weight: 600;
			color: #2a5164;
		}

		.overview-subvalue {
			display: block;
			font-size: 12px;
			color: #5a7a86;
		}

		.rec-grid {
			display: grid;
			grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
			gap: 18px;
		}

		.grid-card {
			display: flex;
			flex-direction: column;
			gap: 12px;
			padding: 18px;
			border-radius: 14px;
			min-height: 200px;
			background: #f4fbf6;
			box-shadow: inset 0 0 0 1px rgba(102, 187, 106, 0.1);
		}

		.grid-title {
			display: flex;
			align-items: center;
			gap: 8px;
			font-weight: 600;
			color: #1b5e20;

			&.warning {
				color: #c62828;
			}

			.el-icon {
				width: 28px;
				height: 28px;
				border-radius: 10px;
				background: rgba(102, 187, 106, 0.15);
				display: flex;
				align-items: center;
				justify-content: center;
			}
		}

		.decorated-list {
			list-style: none;
			margin: 0;
			padding: 0;
			display: flex;
			flex-direction: column;
			gap: 10px;

			li {
				position: relative;
				padding-left: 14px;
				font-size: 13px;
				color: #4a4a4a;
				line-height: 1.6;

				&::before {
					content: '';
					position: absolute;
					left: 0;
					top: 4px;
					width: 6px;
					height: 16px;
					border-radius: 3px;
					background: linear-gradient(180deg, #66bb6a, #43a047);
				}
			}

			&.vertical li::before {
				height: 6px;
				width: 6px;
				top: 10px;
			}
		}

		.cost-highlight {
			display: flex;
			align-items: baseline;
			gap: 8px;

			.amount {
				font-size: 26px;
				font-weight: 700;
				color: #2e7d32;

				&.muted {
					color: #9e9e9e;
				}
			}

			.unit {
				font-size: 12px;
				color: #5a7a86;
			}
		}

		.cost-breakdown {
			margin: 0;
			font-size: 13px;
			color: #4a4a4a;
		}

		.rec-footer {
			display: grid;
			grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
			gap: 18px;
		}

		.footer-block {
			display: flex;
			flex-direction: column;
			gap: 12px;
			padding: 18px;
			border-radius: 14px;
			background: #f7fbff;
			min-height: 160px;
		}

		.footer-title {
			display: flex;
			align-items: center;
			gap: 8px;
			font-weight: 600;
			color: #1b5e20;

			&.warning {
				color: #c62828;
			}
		}

		.placeholder-text {
			color: #9e9e9e;
			font-size: 13px;
		}

		.tag-group {
			display: flex;
			flex-wrap: wrap;
			gap: 10px;
		}
	}

	.weather-card {
		.weather-content {
			display: flex;
			flex-direction: column;
			gap: 24px;
		}

		.weather-top {
			display: flex;
			flex-direction: column;
			gap: 16px;
		}

		.snapshot-grid {
			display: grid;
			grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
			gap: 16px;
		}

		.snapshot-card {
			padding: 18px;
			border-radius: 14px;
			background: linear-gradient(135deg, rgba(129, 199, 132, 0.2), rgba(197, 225, 233, 0.2));
			display: flex;
			flex-direction: column;
			gap: 12px;
		}

		.snapshot-label {
			display: flex;
			align-items: center;
			gap: 8px;
			font-weight: 600;
			color: #1b5e20;
		}

		.snapshot-value {
			display: flex;
			align-items: baseline;
			gap: 6px;

			.number {
				font-size: 28px;
				font-weight: 700;
				color: #2a5164;
			}

			.unit {
				font-size: 12px;
				color: #5a7a86;
			}
		}

		.meta-descriptions {
			:deep(.el-descriptions__label) {
				font-weight: 600;
				color: #437064;
			}

			:deep(.el-descriptions__content) {
				color: #4a4a4a;
			}
		}

		.weather-chart-wrapper {
			width: 100%;
			min-height: 320px;
			padding: 12px;
			border-radius: 14px;
			background: #f5f9ff;
		}

		.weather-chart {
			width: 100%;
			height: 300px;
		}
	}

	.empty-holder {
		padding: 32px 0;
	}

	.line-clamp {
		display: -webkit-box;
		-webkit-line-clamp: 2;
		-webkit-box-orient: vertical;
		overflow: hidden;
	}
}

@media (max-width: 992px) {
	.solution-page {
		.banner-card .banner-content {
			flex-direction: column;
			align-items: flex-start;
		}

		.parameter-card .parameter-content {
			grid-template-columns: 1fr;
		}

		.parameter-card .parameter-actions .action-buttons {
			flex-wrap: wrap;
			justify-content: flex-start;
		}
	}
}

@media (max-width: 680px) {
	.solution-page {
		.recommendation-card .rec-grid,
		.solution-page .recommendation-card .rec-footer,
		.weather-card .snapshot-grid {
			grid-template-columns: 1fr;
		}
	}
}
</style>
