<template>
	<div class="page-shell">
		<el-card shadow="never" class="predict-card">
			<header class="predict-card__intro">
				<div class="predict-card__intro-text">
					<h2>图像识别</h2>
					<p>上传农作物病害图片，系统将调用识别模型并给出预测结果与置信度。</p>
					<p class="predict-card__note">支持 jpg、png 等常见格式，单张图片不超过 5MB。</p>
				</div>
				<el-button type="primary" size="default" class="predict-card__reset recognize-btn" @click="resetForm">
					<el-icon><ele-RefreshRight /></el-icon>
					重置参数
				</el-button>
			</header>

			<div class="predict-card__main">
				<div class="param-header">
					<div class="param-item">
						<el-select
							v-model="kind"
							placeholder="选择作物类别"
							size="default"
							style="width: 180px"
							@change="getData"
						>
							<el-option
								v-for="item in state.kind_items"
								:key="item.value"
								:label="item.label"
								:value="item.value"
							/>
						</el-select>
					</div>
					<div class="param-item">
						<el-select
							v-model="weight"
							placeholder="选择识别模型"
							size="default"
							style="width: 200px"
						>
							<el-option
								v-for="item in state.weight_items"
								:key="item.value"
								:label="item.label"
								:value="item.value"
							/>
						</el-select>
					</div>
					<div class="param-item param-slider">
						<div class="slider-label">置信度阈值</div>
						<el-slider
							v-model="confPercent"
							:min="0"
							:max="100"
							:step="1"
							:format-tooltip="formatTooltip"
							show-stops
							style="width: 200px"
						/>
						<span class="slider-value">{{ confPercent }}%</span>
					</div>
					<div class="param-item">
						<el-button
							class="recognize-btn"
							type="primary"
							size="default"
							@click="upData"
							:loading="state.loading"
							:disabled="!state.img"
						>
							<el-icon><ele-Promotion /></el-icon>
							开始识别
						</el-button>
					</div>
				</div>

				<div class="content-layout">
					<div class="upload-pane">
						<el-upload
							v-model="state.img"
							ref="uploadFile"
							class="upload-box"
							:action="uploadAction"
							:show-file-list="false"
							:on-success="handleAvatarSuccessone"
						>
							<img v-if="imageUrl" :src="imageUrl" class="upload-preview" />
							<div v-else class="upload-placeholder">
								<el-icon class="upload-icon"><Plus /></el-icon>
								<p>拖拽或点击上传图片</p>
								<span>识别完成后结果会自动记录</span>
							</div>
						</el-upload>
					</div>

					<section class="result-section">
						<header class="result-section__header">
							<h3>识别结果</h3>
							<p>模型输出将在此处实时展示，可查看标签、置信度与耗时。</p>
						</header>

						<div v-if="hasResult" class="result-cards">
							<div class="result-card">
								<div class="result-card__label">标签</div>
								<div class="result-card__value">{{ formattedLabel }}</div>
							</div>
							<div class="result-card">
								<div class="result-card__label">置信度</div>
								<div class="result-card__value">{{ state.predictionResult.confidence }}</div>
							</div>
							<div class="result-card">
								<div class="result-card__label">耗时</div>
								<div class="result-card__value">{{ state.predictionResult.allTime }}</div>
							</div>
						</div>

						<div v-else class="result-section__placeholder">
							上传图片并点击开始识别，结果会显示在这里。
						</div>

						<div v-if="hasResult" class="solution-entry">
							<el-button type="success" plain class="solution-entry__button" @click="goToSolutionCenter">
								查看推荐防治方案
							</el-button>
						</div>
					</section>
				</div>
			</div>
		</el-card>
	</div>
</template>

<script setup lang="ts" name="imgPredict">
import { reactive, ref, computed, onMounted } from 'vue';
import type { UploadInstance, UploadProps } from 'element-plus';
import { ElMessage } from 'element-plus';
import request from '/@/utils/request';
import { Plus } from '@element-plus/icons-vue';
import { useUserInfo } from '/@/stores/userInfo';
import { storeToRefs } from 'pinia';
import { formatDate } from '/@/utils/formatTime';
import { useRouter } from 'vue-router';

type KindOption = {
	value: string;
	label: string;
};

type WeightOption = {
	value: string;
	label: string;
};

type PredictionLabel = string | string[];

const router = useRouter();
const imageUrl = ref('');
const conf = ref(0.5);
const weight = ref('');
const kind = ref('');
const uploadFile = ref<UploadInstance>();
const uploadAction = ref('/api/files/upload');
const stores = useUserInfo();
const { userInfos } = storeToRefs(stores);

const state = reactive({
	weight_items: [] as WeightOption[],
	kind_items: [
		{ value: 'corn', label: '玉米' },
		{ value: 'rice', label: '水稻' },
		{ value: 'strawberry', label: '草莓' },
		{ value: 'tomato', label: '西红柿' },
		{ value: 'citrus', label: '柑橘' },
	] as KindOption[],
	img: '',
	predictionResult: {
		label: '' as PredictionLabel,
		confidence: '',
		allTime: '',
	},
	loading: false,
});

const confPercent = computed({
	get: () => Math.min(Math.max(Math.round(conf.value * 100), 0), 100),
	set: (val: number) => {
		const normalized = Math.min(Math.max(val, 0), 100) / 100;
		conf.value = Number(normalized.toFixed(2));
	},
});

const hasResult = computed(() => {
	const label = state.predictionResult.label;
	return Array.isArray(label) ? label.length > 0 : !!label;
});

const formattedLabel = computed(() => {
	const label = state.predictionResult.label;
	if (Array.isArray(label)) {
		return label.join(' / ');
	}
	return label;
});

const decodeUnicode = (value: string) =>
	value.replace(/\\u([\dA-Fa-f]{4})/g, (_, code) => String.fromCharCode(parseInt(code, 16)));

const isSuccessCode = (code: unknown) => code === 0 || code === '0';

const normaliseLabel = (rawLabel: unknown): PredictionLabel => {
	if (Array.isArray(rawLabel)) {
		return rawLabel.map((item) => decodeUnicode(String(item)));
	}
	if (typeof rawLabel === 'string') {
		const trimmed = rawLabel.trim();
		if (!trimmed) {
			return '';
		}
		try {
			const parsed = JSON.parse(trimmed);
			return normaliseLabel(parsed);
		} catch {
			return decodeUnicode(trimmed);
		}
	}
	return '';
};

const formatTooltip = (val: number) => `${val}%`;

const resetForm = () => {
	kind.value = '';
	weight.value = '';
	conf.value = 0.5;
	state.img = '';
	imageUrl.value = '';
	state.predictionResult.label = '';
	state.predictionResult.confidence = '';
	state.predictionResult.allTime = '';
	state.loading = false;
	uploadFile.value?.clearFiles();
	getData();
};

const handleAvatarSuccessone: UploadProps['onSuccess'] = (response, file) => {
	if (file?.raw) {
		imageUrl.value = URL.createObjectURL(file.raw);
	}
	state.img = response?.data || '';
	state.predictionResult.label = '';
	state.predictionResult.confidence = '';
	state.predictionResult.allTime = '';
};

const parsePossibleJsonArray = (input: unknown): unknown => {
	if (!input) return input;
	if (Array.isArray(input)) return input;
	if (typeof input === 'string') {
		const trimmed = input.trim();
		if (!trimmed) return [];
		try {
			const parsed = JSON.parse(trimmed);
			return Array.isArray(parsed) ? parsed : [parsed];
		} catch (error) {
			console.warn('模型列表格式异常，尝试按逗号拆分:', error);
			return trimmed
				.split(',')
				.map((segment) => segment.trim())
				.filter(Boolean);
		}
	}
	return input;
};

const normaliseWeightItems = (items: unknown): WeightOption[] => {
	const source = parsePossibleJsonArray(items);
	if (!Array.isArray(source)) return [];
	return source
		.map((item) => {
			if (item && typeof item === 'object') {
				const value = String((item as WeightOption).value ?? '');
				const label = String((item as WeightOption).label ?? value);
				return value ? { value, label } : null;
			}
			const value = String(item ?? '');
			return value ? { value, label: value } : null;
		})
		.filter((item): item is WeightOption => !!item);
};
const getData = () => {
	request
		.get('/api/flask/file_names')
		.then((res) => {
			if (isSuccessCode(res.code)) {
				try {
					const payload = typeof res.data === 'string' ? JSON.parse(res.data) : res.data;
					const items = normaliseWeightItems(payload?.weight_items);
					const keyword = (kind.value || '').toLowerCase();
					const filtered = keyword
						? items.filter((item) => item.value.toLowerCase().includes(keyword))
						: items;
					const finalItems = filtered.length ? filtered : items;
					state.weight_items = finalItems;
					if (weight.value && finalItems.every((item) => item.value !== weight.value)) {
						weight.value = '';
					}
				} catch (error) {
					console.error('解析模型列表失败:', error);
					state.weight_items = [];
					weight.value = '';
				}
			} else {
				ElMessage.error(res.msg || '获取模型列表失败');
				state.weight_items = [];
				weight.value = '';
			}
		})
		.catch((error) => {
			console.error('获取模型列表接口异常:', error);
			ElMessage.error('获取模型列表失败，请稍后重试。');
			state.weight_items = [];
			weight.value = '';
		});
};

const upData = async () => {
	if (!state.img) {
		ElMessage.warning('请先上传图片。');
		return;
	}
	if (!weight.value) {
		ElMessage.warning('请选择识别模型。');
		return;
	}
	if (!kind.value) {
		ElMessage.warning('请选择作物类别。');
		return;
	}

	state.loading = true;
	const formPayload = {
		username: userInfos.value.userName,
		inputImg: state.img,
		weight: weight.value,
		conf: Number(conf.value.toFixed(2)),
		kind: kind.value,
		startTime: formatDate(new Date(), 'YYYY-mm-dd HH:MM:SS'),
	};

	try {
		const res = await request.post('/api/flask/predict', formPayload);
		if (isSuccessCode(res.code)) {
			const rawData = typeof res.data === 'string' ? JSON.parse(res.data) : res.data;
			state.predictionResult.label = normaliseLabel(rawData?.label);
			state.predictionResult.confidence = rawData?.confidence ?? '';
			state.predictionResult.allTime = rawData?.allTime ?? '';
			if (rawData?.outImg) {
				imageUrl.value = rawData.outImg;
			}
			ElMessage.success('识别完成。');
		} else {
			ElMessage.error(res.msg || '识别失败，请稍后重试。');
		}
	} catch (error) {
		console.error('识别接口调用异常:', error);
		ElMessage.error('识别过程中出现异常。');
	} finally {
		state.loading = false;
	}
};

const goToSolutionCenter = () => {
	router.push('/solution/plan');
};

onMounted(() => {
	getData();
});
</script>

<style scoped lang="scss">
.page-shell {
	width: 100%;
	min-height: 100vh;
	padding: 24px;
	display: flex;
	flex-direction: column;
	align-items: center;
	background: radial-gradient(circle, #e3f7ef 0%, #ffffff 100%);
	box-sizing: border-box;
}

.predict-card {
	flex: 1;
	width: 100%;
	max-width: 1360px;
	border-radius: 20px;
	padding: 36px;
	display: flex;
	flex-direction: column;
	row-gap: 36px;
	min-height: 0;
	box-sizing: border-box;
}

.predict-card__intro {
	display: flex;
	justify-content: space-between;
	align-items: flex-start;
	gap: 16px;
}

.predict-card__intro-text h2 {
	margin: 0;
	font-size: 24px;
	color: #1a745d;
}

.predict-card__intro-text p {
	margin: 4px 0 0;
	color: #606266;
	font-size: 14px;
	line-height: 1.5;
}

.predict-card__note {
	font-size: 13px;
	color: #909399;
}

.predict-card__reset {
	display: inline-flex;
	align-items: center;
	gap: 6px;
}

.predict-card__main {
	display: flex;
	flex-direction: column;
	row-gap: 32px;
	flex: 1;
	min-height: 0;
}

.param-header {
	display: flex;
	flex-wrap: wrap;
	gap: 16px;
	align-items: center;
}

.param-item {
	display: flex;
	align-items: center;
}

.param-slider {
	gap: 12px;
}

.slider-label {
	min-width: 120px;
	font-size: 14px;
	color: #909399;
}

.slider-value {
	min-width: 52px;
	font-weight: 600;
	color: #1a745d;
	text-align: right;
}

.recognize-btn {
	display: inline-flex;
	align-items: center;
	gap: 6px;
	font-weight: 600;
}

.content-layout {
	display: flex;
	flex: 1;
	min-height: 600px;
	gap: 36px;
	overflow: hidden;
}

.upload-pane {
	flex: 3.5 1 0;
	display: flex;
	justify-content: center;
	align-items: center;
	min-height: 0;
	max-height: 100%;
}

.upload-box {
	width: 100%;
	max-width: 640px;
	height: 100%;
	display: flex;
	justify-content: center;
	align-items: center;
	padding: 16px;
	aspect-ratio: 4 / 3;
}

.upload-preview {
	width: 100%;
	height: 100%;
	border-radius: 12px;
	object-fit: contain;
	box-shadow: 0 12px 24px rgba(0, 0, 0, 0.12);
	display: block;
	background: #fff;
}

.upload-placeholder {
	width: 100%;
	max-width: 640px;
	border: 1px dashed #c0c4cc;
	border-radius: 12px;
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	gap: 8px;
	background: rgba(26, 116, 93, 0.05);
	color: #606266;
	font-size: 14px;
	text-align: center;
	padding: 24px;
	box-sizing: border-box;
	aspect-ratio: 4 / 3;
}

.upload-icon {
	font-size: 32px;
	color: #1a745d;
}

.result-section {
	flex: 2.2 1 0;
	display: flex;
	flex-direction: column;
	row-gap: 20px;
	min-height: 0;
	max-height: 100%;
	overflow-y: auto;
	padding-right: 4px;
}

.result-section__header h3 {
	margin: 0;
	font-size: 20px;
	color: #1f2f3d;
}

.result-section__header p {
	margin: 4px 0 0;
	font-size: 14px;
	color: #909399;
}

.result-cards {
	display: flex;
	flex-direction: column;
	gap: 16px;
}

.result-card {
	background: linear-gradient(135deg, rgba(26, 116, 93, 0.08), rgba(26, 116, 93, 0.02));
	border-radius: 14px;
	padding: 18px 20px;
	display: flex;
	flex-direction: column;
	gap: 8px;
	box-shadow: inset 0 0 0 1px rgba(26, 116, 93, 0.18);
}

.result-card__label {
	font-size: 13px;
	color: #1a745d;
	letter-spacing: 0.6px;
	font-weight: 600;
	text-transform: uppercase;
}

.result-card__value {
	font-size: 20px;
	font-weight: 600;
	color: #0f4f3c;
	word-break: break-word;
	line-height: 1.4;
}

.result-section__placeholder {
	padding: 24px 20px;
	border-radius: 14px;
	background: #f5f7fa;
	color: #909399;
	font-size: 14px;
	min-height: 180px;
	display: flex;
	align-items: center;
	justify-content: center;
	text-align: center;
}

.solution-entry {
	display: flex;
	justify-content: flex-end;
}

.solution-entry__button {
	font-weight: 600;
}

@media (max-width: 960px) {
	.predict-card {
		padding: 16px;
		min-height: auto;
	}

	.param-slider {
		width: 100%;
		justify-content: space-between;
	}

	.slider-label {
		min-width: auto;
	}

	.content-layout {
		flex-direction: column;
	}

	.upload-pane,
	.result-section {
		flex: none;
		max-height: none;
	}
}
</style>
