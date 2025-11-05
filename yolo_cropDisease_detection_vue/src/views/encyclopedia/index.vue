<template>
	<div class="encyclopedia-page" v-loading="state.loading">
		<el-card shadow="hover" class="hero-card">
			<div class="hero-content">
				<div class="hero-text">
					<div class="hero-title">
						<h2>农作物病害知识百科</h2>
						<p>汇集重点作物病害的症状、病原与气候风险，为巡田巡检与诊断提供权威参考。</p>
					</div>
					<div class="hero-search">
						<el-input
							v-model="state.searchKeyword"
							placeholder="输入病害名称、编号或作物名称进行搜索"
							class="search-input"
							clearable
							@keyup.enter="handleSearch"
							@clear="handleSearchReset"
						>
							<template #prefix>
								<el-icon><Search /></el-icon>
							</template>
						</el-input>
						<el-button type="primary" :loading="state.searchLoading" @click="handleSearch">
							<el-icon><Search /></el-icon>
							<span>搜索</span>
						</el-button>
					</div>
				</div>
				<div class="hero-stats">
					<div class="stat-card" v-for="item in overviewStats" :key="item.label">
						<div class="stat-label">{{ item.label }}</div>
						<div class="stat-value">
							<span>{{ item.value }}</span>
							<small>{{ item.unit }}</small>
						</div>
					</div>
				</div>
			</div>
		</el-card>

		<el-card shadow="never" class="tabs-card">
			<el-tabs v-model="state.activeTab">
				<el-tab-pane label="全部病害" name="ALL" />
				<el-tab-pane
					v-if="state.searchResults.length"
					:label="`搜索结果 (${state.searchResults.length})`"
					:name="SEARCH_TAB"
				/>
				<el-tab-pane
					v-for="group in state.data?.crops || []"
					:key="group.cropName"
					:label="`${group.cropDisplayName || group.cropName} (${group.diseaseCount})`"
					:name="group.cropName"
				/>
			</el-tabs>
		</el-card>

		<div class="content-layout">
			<div class="list-column">
				<el-card shadow="never" class="filter-card">
					<div class="filter-header">
						<div class="filter-title">
							<el-icon><Filter /></el-icon>
							<span>筛选条件</span>
						</div>
						<el-button text type="primary" @click="resetFilters">重置</el-button>
					</div>
					<div class="filter-controls">
						<el-select v-model="state.pathogenFilter" placeholder="病原类型" size="large" class="filter-select">
							<el-option
								v-for="option in pathogenOptions"
								:key="option.value"
								:label="option.label"
								:value="option.value"
							/>
						</el-select>
						<el-select v-model="state.riskFilter" placeholder="风险等级" size="large" class="filter-select">
							<el-option
								v-for="option in riskOptions"
								:key="option.value"
								:label="option.label"
								:value="option.value"
							/>
						</el-select>
					</div>
				</el-card>

				<el-card shadow="hover" class="list-card">
					<template #header>
						<div class="list-header">
							<div class="list-title">
								<el-icon><Collection /></el-icon>
								<span>{{ listHeaderTitle }}</span>
							</div>
							<el-tag v-if="filteredDiseases.length" size="small" type="success">
								共 {{ filteredDiseases.length }} 条
							</el-tag>
						</div>
					</template>

					<div class="list-body" v-if="filteredDiseases.length">
						<el-scrollbar>
							<div
								v-for="disease in filteredDiseases"
								:key="diseaseKey(disease)"
								:class="['disease-item', { active: diseaseKey(disease) === state.selectedDiseaseKey }]"
								@click="selectDisease(disease)"
							>
								<div class="disease-item__header">
									<h4>{{ disease.diseaseName }}</h4>
									<el-tag size="small" :type="riskTagType(disease.riskLevel)">
										{{ riskLabel(disease.riskLevel) }}
									</el-tag>
								</div>
								<div class="disease-item__meta">
									<el-tag size="small" effect="plain">{{ disease.cropName || '未分类' }}</el-tag>
									<span class="disease-code">{{ disease.diseaseCode }}</span>
								</div>
								<p class="disease-item__summary">
									{{ disease.symptomSummary || '暂无症状描述' }}
								</p>
							</div>
						</el-scrollbar>
					</div>
					<el-empty
						v-else
						description="未找到匹配的病害记录，尝试调整关键字或筛选条件。"
						:image-size="120"
					/>
				</el-card>
			</div>

			<div class="detail-column">
				<el-card v-if="selectedDisease" shadow="hover" class="detail-card highlight-card">
					<div class="detail-header">
						<div>
							<h3>{{ selectedDisease.diseaseName }}</h3>
							<p>{{ activeCropDisplayName }}</p>
						</div>
						<div class="detail-tags">
							<el-tag size="small" :type="riskTagType(selectedDisease.riskLevel)" effect="dark">
								{{ riskLabel(selectedDisease.riskLevel) }}
							</el-tag>
							<el-tag size="small" effect="dark" type="info">
								{{ pathogenLabel(selectedDisease.pathogenType) }}
							</el-tag>
						</div>
					</div>
					<div class="detail-meta">
						<div class="meta-item">
							<span class="meta-label">病害编号</span>
							<span class="meta-value">{{ selectedDisease.diseaseCode || '-' }}</span>
						</div>
						<div class="meta-item">
							<span class="meta-label">风险等级</span>
							<span class="meta-value">{{ riskLabel(selectedDisease.riskLevel) }}</span>
						</div>
						<div class="meta-item">
							<span class="meta-label">最近更新</span>
							<span class="meta-value">{{ formatDateText(selectedDisease.updatedAt) }}</span>
						</div>
					</div>
					<p class="detail-description">
						{{ selectedDisease.description || '暂无详细描述' }}
					</p>
				</el-card>

				<el-card v-if="selectedDisease" shadow="hover" class="detail-card">
					<template #header>
						<div class="detail-title">
							<el-icon><Reading /></el-icon>
							<span>症状表现</span>
						</div>
					</template>
					<p class="detail-content">
						{{ selectedDisease.symptomSummary || '暂无症状信息，建议结合现场表现进行判断。' }}
					</p>
				</el-card>

				<el-card v-if="selectedDisease" shadow="hover" class="detail-card">
					<template #header>
						<div class="detail-title">
							<el-icon><Histogram /></el-icon>
							<span>病原特点</span>
						</div>
					</template>
					<p class="detail-content">
						{{ pathogenDescription(selectedDisease.pathogenType) }}
					</p>
				</el-card>

				<el-card v-if="selectedDisease" shadow="hover" class="detail-card">
					<template #header>
						<div class="detail-title">
							<el-icon><Sunny /></el-icon>
							<span>气候风险因素</span>
						</div>
					</template>
					<p class="detail-content">
						{{ selectedDisease.climateRiskFactors || '暂无气候风险信息，请结合当地天气动态评估。' }}
					</p>
				</el-card>

				<el-card v-else shadow="never" class="detail-placeholder">
					<el-empty description="请选择左侧病害查看详情" :image-size="200" />
				</el-card>
			</div>
		</div>
	</div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { Search, Collection, Sunny, Filter, Reading, Histogram } from '@element-plus/icons-vue';
import type { DiseaseInfo } from '/@/api/disease';
import {
	getEncyclopediaData,
	searchDiseases,
	type DiseaseEncyclopediaPayload,
} from '/@/api/encyclopedia';
import { formatDate } from '/@/utils/formatTime';

const SEARCH_TAB = 'SEARCH';

const state = reactive({
	loading: false,
	searchLoading: false,
	data: null as DiseaseEncyclopediaPayload | null,
	activeTab: 'ALL',
	searchKeyword: '',
	pathogenFilter: 'ALL',
	riskFilter: 'ALL',
	searchResults: [] as DiseaseInfo[],
	selectedDiseaseKey: null as string | null,
});

const overviewStats = computed(() => {
	const overview = state.data?.overview || {
		totalDiseases: 0,
		cropCount: 0,
		highRiskCount: 0,
		mediumRiskCount: 0,
		lowRiskCount: 0,
	};
	return [
		{ label: '收录病害', value: overview.totalDiseases, unit: '种' },
		{ label: '覆盖作物', value: overview.cropCount, unit: '类' },
		{ label: '高风险病害', value: overview.highRiskCount, unit: '种' },
		{ label: '中低风险', value: overview.mediumRiskCount + overview.lowRiskCount, unit: '种' },
	];
});

const pathogenOptions = computed(() => {
	const list = state.data?.pathogenTypes || [];
	return [{ value: 'ALL', label: '全部病原类型' }].concat(
		list.map((item) => ({
			value: item,
			label: pathogenLabel(item),
		}))
	);
});

const riskOptions = computed(() => {
	const list = state.data?.riskLevels || [];
	return [{ value: 'ALL', label: '全部风险等级' }].concat(
		list.map((item) => ({
			value: item,
			label: riskLabel(item),
		}))
	);
});

const allDiseases = computed(() => {
	if (!state.data) return [] as DiseaseInfo[];
	return state.data.crops.flatMap((group) => group.diseases || []);
});

const sourceDiseases = computed(() => {
	if (state.activeTab === SEARCH_TAB) {
		return state.searchResults;
	}
	if (state.activeTab === 'ALL') {
		return allDiseases.value;
	}
	const target = state.data?.crops.find((group) => group.cropName === state.activeTab);
	return target?.diseases || [];
});

const filteredDiseases = computed(() => {
	const keyword = state.searchKeyword.trim().toLowerCase();
	return sourceDiseases.value.filter((disease) => {
		const matchesKeyword =
			!keyword ||
			[
				disease.diseaseName,
				disease.diseaseCode,
				disease.cropName,
				disease.symptomSummary,
			]
				.map((item) => (item ? String(item).toLowerCase() : ''))
				.some((field) => field.includes(keyword));
		const matchesPathogen =
			state.pathogenFilter === 'ALL' ||
			(state.pathogenFilter &&
				String(disease.pathogenType || '').toUpperCase() === state.pathogenFilter.toUpperCase());
		const matchesRisk =
			state.riskFilter === 'ALL' ||
			(state.riskFilter && String(disease.riskLevel || '').toUpperCase() === state.riskFilter.toUpperCase());
		return matchesKeyword && matchesPathogen && matchesRisk;
	});
});

const selectedDisease = computed(() => {
	if (!filteredDiseases.value.length) return null;
	const target = filteredDiseases.value.find((item) => diseaseKey(item) === state.selectedDiseaseKey);
	return target || filteredDiseases.value[0];
});

const activeCropDisplayName = computed(() => {
	if (state.activeTab === SEARCH_TAB || state.activeTab === 'ALL') {
		return selectedDisease.value?.cropName || '多作物';
	}
	const target = state.data?.crops.find((group) => group.cropName === state.activeTab);
	return target?.cropDisplayName || target?.cropName || '多作物';
});

const listHeaderTitle = computed(() => {
	if (state.activeTab === 'ALL') return '全部病害列表';
	if (state.activeTab === SEARCH_TAB) return '搜索结果';
	const target = state.data?.crops.find((group) => group.cropName === state.activeTab);
	return target ? `${target.cropDisplayName || target.cropName}病害` : '病害列表';
});

watch(
	filteredDiseases,
	(list) => {
		if (!list.length) {
			state.selectedDiseaseKey = null;
			return;
		}
		if (!state.selectedDiseaseKey || !list.some((item) => diseaseKey(item) === state.selectedDiseaseKey)) {
			state.selectedDiseaseKey = diseaseKey(list[0]);
		}
	},
	{ immediate: true }
);

const fetchData = async () => {
	state.loading = true;
	try {
		const res: any = await getEncyclopediaData();
		if (res.code === '0') {
			state.data = res.data;
		} else {
			ElMessage.error(res.msg || '获取病害百科数据失败');
		}
	} catch (error) {
		ElMessage.error('获取病害百科数据失败，请稍后重试');
	} finally {
		state.loading = false;
	}
};

const handleSearch = async () => {
	const keyword = state.searchKeyword.trim();
	if (!keyword) {
		handleSearchReset();
		return;
	}
	state.searchLoading = true;
	try {
		const res: any = await searchDiseases(keyword);
		if (res.code === '0') {
			state.searchResults = res.data || [];
			state.activeTab = SEARCH_TAB;
			state.pathogenFilter = 'ALL';
			state.riskFilter = 'ALL';
		} else {
			ElMessage.error(res.msg || '搜索失败，请稍后重试');
		}
	} catch (error) {
		ElMessage.error('搜索失败，请检查网络后重试');
	} finally {
		state.searchLoading = false;
	}
};

const handleSearchReset = () => {
	state.searchKeyword = '';
	state.searchResults = [];
	if (state.activeTab === SEARCH_TAB) {
		state.activeTab = 'ALL';
	}
};

const resetFilters = () => {
	state.pathogenFilter = 'ALL';
	state.riskFilter = 'ALL';
};

const selectDisease = (disease: DiseaseInfo) => {
	state.selectedDiseaseKey = diseaseKey(disease);
};

const diseaseKey = (disease: DiseaseInfo) => String(disease.id ?? disease.diseaseCode ?? disease.diseaseName ?? '');

const riskLabel = (risk?: string) => {
	switch ((risk || '').toUpperCase()) {
		case 'HIGH':
			return '高风险';
		case 'MEDIUM':
			return '中风险';
		case 'LOW':
			return '低风险';
		default:
			return risk || '未知';
	}
};

const riskTagType = (risk?: string) => {
	switch ((risk || '').toUpperCase()) {
		case 'HIGH':
			return 'danger';
		case 'MEDIUM':
			return 'warning';
		case 'LOW':
			return 'success';
		default:
			return 'info';
	}
};

const pathogenLabel = (pathogen?: string) => {
	const value = (pathogen || '').toLowerCase();
	const mapping: Record<string, string> = {
		fungus: '真菌病原',
		bacteria: '细菌病原',
		virus: '病毒病原',
		oomycete: '卵菌病原',
		insect: '虫害传播',
		mite: '螨害传播',
	};
	return mapping[value] || pathogen || '未分类';
};

const pathogenDescription = (pathogen?: string) => {
	const value = (pathogen || '').toLowerCase();
	const mapping: Record<string, string> = {
		fungus: '真菌性病害多在潮湿环境下扩散，注意通风降湿，并结合保护性杀菌剂进行预防。',
		bacteria: '细菌性病害常由雨水飞溅或机械伤口传播，需及时清沟排水并选用铜制剂或抗生素类药剂。',
		virus: '病毒性病害通常经蚜虫、粉虱等介体传播，重点清除毒源植株并加强虫害监测。',
		oomycete: '卵菌性病害喜凉湿环境，建议关注夜间露水、降雨过程并及时使用针对性药剂。',
		insect: '虫害传播型病害需同步进行虫口监测与化学/生物防治，阻断传播介体。',
		mite: '螨害传播需在干燥高温条件下加强植株洗尘和螨药使用频次。',
	};
	return mapping[value] || '暂无病原说明，请结合实际调查情况选择针对性防控策略。';
};

const formatDateText = (value?: string) => {
	if (!value) return '未记录';
	const date = new Date(value);
	if (Number.isNaN(date.getTime())) return '未记录';
	return formatDate(date, 'YYYY-mm-dd HH:MM');
};

onMounted(() => {
	fetchData();
});
</script>

<style scoped lang="scss">
.encyclopedia-page {
	display: flex;
	flex-direction: column;
	gap: 20px;
	padding: 20px;
	background: #f5f7f6;
	min-height: 100%;
	box-sizing: border-box;
}

.hero-card {
	background: linear-gradient(135deg, rgba(32, 201, 151, 0.1), rgba(32, 201, 151, 0.05));
	border: 1px solid rgba(32, 201, 151, 0.15);

	.hero-content {
		display: flex;
		align-items: stretch;
		justify-content: space-between;
		gap: 40px;
	}

	.hero-text {
		flex: 1.2;
		display: flex;
		flex-direction: column;
		gap: 16px;

		h2 {
			margin: 0;
			font-size: 28px;
			color: #1b4d3e;
		}

		p {
			margin: 0;
			color: #4f7264;
			line-height: 1.6;
		}

		.hero-search {
			display: flex;
			gap: 12px;
			align-items: center;
			margin-top: 4px;
		}

		.search-input {
			flex: 1;
		}
	}

	.hero-stats {
		flex: 0.9;
		display: grid;
		grid-template-columns: repeat(2, minmax(140px, 1fr));
		gap: 16px;

		.stat-card {
			background: #ffffff;
			border-radius: 14px;
			padding: 16px;
			display: flex;
			flex-direction: column;
			gap: 10px;
			box-shadow: inset 0 0 0 1px rgba(32, 201, 151, 0.12);

			.stat-label {
				font-size: 14px;
				color: #4f7264;
			}

			.stat-value {
				display: flex;
				align-items: baseline;
				gap: 6px;
				color: #1b4d3e;

				span {
					font-size: 30px;
					font-weight: 700;
				}

				small {
					font-size: 13px;
					color: #719488;
				}
			}
		}
	}
}

.tabs-card {
	border-radius: 16px;
	padding: 0 6px 6px;
}

.content-layout {
	display: grid;
	grid-template-columns: minmax(280px, 32%) 1fr;
	gap: 20px;
	align-items: start;
}

.list-column {
	display: flex;
	flex-direction: column;
	gap: 16px;

	.filter-card {
		background: #ffffff;
		border-radius: 14px;

		.filter-header {
			display: flex;
			justify-content: space-between;
			align-items: center;
			margin-bottom: 16px;

			.filter-title {
				display: flex;
				align-items: center;
				gap: 8px;
				font-weight: 600;
				color: #205c4c;
			}
		}

		.filter-controls {
			display: grid;
			gap: 12px;
		}

		.filter-select {
			width: 100%;
		}
	}

	.list-card {
		border-radius: 14px;

		.list-header {
			display: flex;
			justify-content: space-between;
			align-items: center;

			.list-title {
				display: flex;
				align-items: center;
				gap: 8px;
				color: #205c4c;
				font-weight: 600;
			}
		}

		.list-body {
			max-height: 520px;
			min-height: 300px;

			:deep(.el-scrollbar__view) {
				display: flex;
				flex-direction: column;
				gap: 12px;
				padding-right: 6px;
			}
		}
	}
}

.disease-item {
	border: 1px solid transparent;
	border-radius: 12px;
	padding: 14px;
	background: #f6faf9;
	display: flex;
	flex-direction: column;
	gap: 10px;
	cursor: pointer;
	transition: all 0.2s ease;

	.disease-item__header {
		display: flex;
		justify-content: space-between;
		align-items: center;
		gap: 12px;

		h4 {
			margin: 0;
			font-size: 16px;
			color: #204c3a;
		}
	}

	.disease-item__meta {
		display: flex;
		align-items: center;
		gap: 8px;
		color: #708b80;
		font-size: 13px;

		.disease-code {
			font-family: 'Fira Code', Menlo, Consolas, monospace;
			padding: 2px 8px;
			border-radius: 10px;
			background: rgba(32, 201, 151, 0.08);
			color: #1f694f;
		}
	}

	.disease-item__summary {
		margin: 0;
		color: #5b6f68;
		font-size: 13px;
		line-height: 1.6;
		max-height: 66px;
		overflow: hidden;
	}

	&:hover {
		transform: translateY(-2px);
		box-shadow: 0 10px 18px rgba(32, 201, 151, 0.12);
		border-color: rgba(32, 201, 151, 0.3);
	}

	&.active {
		border-color: #20c997;
		background: #ffffff;
		box-shadow: 0 12px 22px rgba(32, 201, 151, 0.18);
	}
}

.detail-column {
	display: flex;
	flex-direction: column;
	gap: 16px;

	.detail-card {
		border-radius: 16px;

		.detail-title {
			display: flex;
			align-items: center;
			gap: 8px;
			font-weight: 600;
			color: #205c4c;
		}

		.detail-content {
			margin: 0;
			color: #4a5e56;
			line-height: 1.8;
			white-space: pre-line;
		}
	}

	.highlight-card {
		background: linear-gradient(135deg, rgba(32, 201, 151, 0.18), rgba(32, 201, 151, 0.05));
		border: 1px solid rgba(32, 201, 151, 0.25);
		color: #1b4d3e;

		.detail-header {
			display: flex;
			justify-content: space-between;
			align-items: flex-start;
			gap: 24px;

			h3 {
				margin: 0 0 4px;
				font-size: 24px;
				font-weight: 700;
				color: #144c3d;
			}

			p {
				margin: 0;
				color: #2c6a58;
			}
		}

		.detail-tags {
			display: flex;
			align-items: center;
			gap: 8px;
		}

		.detail-meta {
			display: grid;
			grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
			gap: 12px;
			margin: 16px 0;

			.meta-item {
				display: flex;
				flex-direction: column;
				gap: 6px;

				.meta-label {
					font-size: 13px;
					color: #2f6f5c;
				}

				.meta-value {
					font-size: 16px;
					font-weight: 600;
					color: #123f33;
				}
			}
		}

		.detail-description {
			margin: 0;
			font-size: 15px;
			line-height: 1.8;
			color: #1f4f40;
		}
	}

	.detail-placeholder {
		border-radius: 16px;
		min-height: 380px;
		display: flex;
		align-items: center;
		justify-content: center;
	}
}

@media (max-width: 1180px) {
	.hero-card {
		.hero-content {
			flex-direction: column;
		}

		.hero-stats {
			width: 100%;
			grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
		}
	}

	.content-layout {
		grid-template-columns: 1fr;
	}
}

@media (max-width: 720px) {
	.hero-card {
		.hero-text {
			h2 {
				font-size: 24px;
			}
			.hero-search {
				flex-direction: column;
				align-items: stretch;
			}
		}
	}

	.detail-column .highlight-card .detail-header {
		flex-direction: column;
		align-items: flex-start;
		gap: 12px;
	}
}
</style>
