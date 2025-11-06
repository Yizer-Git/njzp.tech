<template>
	<div class="system-predict-container layout-padding">
		<div class="system-predict-padding layout-padding-auto layout-padding-view">
			<div class="header">
				<div class="kind">
					<el-select v-model="kind" placeholder="请选择作物种类" size="large" style="width: 180px" @change="getData">
						<el-option v-for="item in state.kind_items" :key="item.value" :label="item.label"
							:value="item.value" />
					</el-select>
				</div>
				<div class="weight">
					<el-select v-model="weight" placeholder="请选择模型" size="large" style="margin-left: 20px;width: 180px">
						<el-option v-for="item in state.weight_items" :key="item.value" :label="item.label"
							:value="item.value" />
					</el-select>
				</div>
				<div class="camera" style="margin-left: 20px; display: flex; align-items: center;">
					<el-select
						v-model="cameraIndex"
						placeholder="请选择摄像头"
						size="large"
						style="width: 220px"
						:loading="state.cameraLoading"
						@visible-change="(visible) => visible && fetchCameras()"
					>
						<el-option
							v-for="item in state.camera_items"
							:key="item.value"
							:label="item.label"
							:value="item.value"
						/>
					</el-select>
					<el-tooltip content="刷新摄像头列表" placement="top">
						<el-button
							circle
							style="margin-left: 8px;"
							:loading="state.cameraLoading"
							@click="fetchCameras"
						>
							<el-icon><ele-Refresh /></el-icon>
						</el-button>
					</el-tooltip>
				</div>
				<div class="conf" style="margin-left: 20px;display: flex; flex-direction: row;align-items: center;">
					<div
						style="font-size: 14px;margin-right: 20px;display: flex;justify-content: start;align-items: center;color: #909399;">
						设置最小置信度阈值</div>
					<el-slider
						v-model="conf"
						:min="0"
						:max="100"
						:step="1"
						:format-tooltip="formatTooltip"
						show-stops
						style="width: 220px;"
					/>
					<span style="margin-left: 12px; min-width: 48px; font-weight: 600; color: #1a745d;">{{ conf }}%</span>
				</div>
				<div class="button-section" style="margin-left: 20px">
					<el-button type="primary" @click="start" class="predict-button">开始录制</el-button>
				</div>
                <div class="button-section" style="margin-left: 20px">
					<el-button type="primary" @click="stop" class="predict-button">结束录制</el-button>
				</div>
				<div class="demo-progress" v-if="state.isShow">
					<el-progress :text-inside="true" :stroke-width="20" :percentage=state.percentage style="width: 380px;">
						<span>{{ state.type_text }} {{ state.percentage }}%</span>
					</el-progress>
				</div>
			</div>
			<div class="cards" ref="cardsContainer">
				<img v-if="state.cameraisShow" class="video" :src="state.video_path">
			</div>
		</div>
	</div>
</template>


<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import request from '/@/utils/request';
import { useUserInfo } from '/@/stores/userInfo';
import { storeToRefs } from 'pinia';
import type { UploadInstance, UploadProps } from 'element-plus';
import { SocketService } from '/@/utils/socket';
import { formatDate } from '/@/utils/formatTime';

const stores = useUserInfo();
const conf = ref(50);
const kind = ref('');
const weight = ref('');
const cameraIndex = ref<number | ''>('');
const { userInfos } = storeToRefs(stores);

const state = reactive({
	weight_items: [] as any,
	kind_items: [
		{
			value: 'corn',
			label: '玉米',
		},
		{
			value: 'rice',
			label: '水稻',
		},
		{
			value: 'strawberry',
			label: '草莓',
		},
		{
			value: 'tomato',
			label: '西红柿',
		},
		{
			value: 'citrus',
			label: '柑橘',
		},
	],
	data: {} as any,
	video_path: '',
	type_text: "正在保存",
	percentage: 50,
	isShow: false,
	cameraisShow: false,
	form: {
		username: '',
		weight: '',
		conf: null as any,
		kind: '',
		startTime: '',
		cameraIndex: ''
	},
	camera_items: [] as any,
	cameraLoading: false,
});

const socketService = SocketService.getInstance();

// 使用防抖来避免重复消息
let messageTimeout: NodeJS.Timeout | null = null;
let lastMessage = '';

socketService.on('message', (data) => {
	console.log('Received message:', data);
	
	// 防抖处理：如果消息相同且时间间隔很短，则忽略
	if (data === lastMessage && messageTimeout) {
		return;
	}
	
	lastMessage = data;
	
	// 清除之前的定时器
	if (messageTimeout) {
		clearTimeout(messageTimeout);
	}
	
	// 设置新的定时器
	messageTimeout = setTimeout(() => {
		ElMessage.success(data);
		messageTimeout = null;
	}, 100);
});

const formatTooltip = (val: number) => {
	return `${val}%`;
};

// 进度消息防抖处理
let progressTimeout: NodeJS.Timeout | null = null;
let lastProgressMessage = '';

socketService.on('progress', (data) => {
	console.log('Received progress:', data);
	
	// 防抖处理：如果消息相同且时间间隔很短，则忽略
	if (data === lastProgressMessage && progressTimeout) {
		return;
	}
	
	lastProgressMessage = data;
	
	// 清除之前的定时器
	if (progressTimeout) {
		clearTimeout(progressTimeout);
	}
	
	// 设置新的定时器
	progressTimeout = setTimeout(() => {
		state.percentage = parseInt(data);
		if (parseInt(data) < 100) {
			state.isShow = true;
		} else {
			//两秒后隐藏进度条
			ElMessage.success("保存成功！");
			setTimeout(() => {
				state.isShow = false;
				state.percentage = 0;
			}, 2000);
		}
		progressTimeout = null;
	}, 50);
});

socketService.on('camera_status', (payload: any) => {
	if (!payload) return;
	const status = payload.status;
	console.log('camera_status:', payload);
	if (status === 'streaming') {
		state.cameraisShow = true;
		if (payload.index !== undefined) {
			ElMessage.success(`摄像头已连接，使用索引 ${payload.index}`);
		}
	} else if (status === 'probing') {
		console.table(payload.attempts || []);
	} else if (status === 'error') {
		state.cameraisShow = false;
		state.video_path = '';
		const msg = payload.message || '摄像头不可用，请检查连接。';
		ElMessage.error(msg);
	} else if (status === 'stopped') {
		state.cameraisShow = false;
	}
});

const getData = () => {
	request.get('/api/flask/file_names').then((res) => {
		if (res.code == 0) {
			res.data = JSON.parse(res.data);
			state.weight_items = res.data.weight_items.filter(item => item.value.includes(kind.value));
		} else {
			ElMessage.error(res.msg);
		}
	});
};

const fetchCameras = () => {
	if (state.cameraLoading) return;
	state.cameraLoading = true;
	request
		.get('/flask/cameras', { params: { limit: 8 } })
			.then((res) => {
				const payload = typeof res === 'string' ? JSON.parse(res) : res;
				if (payload.code === 0 && payload.data) {
					if (Array.isArray(payload.data.diagnostics) && payload.data.diagnostics.length) {
						console.table(payload.data.diagnostics);
					}
					state.camera_items = (payload.data.cameras || []).map((item: any) => {
						const resolution = item.resolution || {};
						const width = resolution.width || 0;
						const height = resolution.height || 0;
						const resolutionText = width && height ? `（${width}×${height}）` : '';
					return {
						value: item.index,
						label: `${item.label || `摄像头 ${item.index}`}${resolutionText}`,
					};
				});
				if (state.camera_items.length > 0) {
					const matched = state.camera_items.find((item: any) => item.value === cameraIndex.value);
					cameraIndex.value = matched ? matched.value : state.camera_items[0].value;
				} else {
					cameraIndex.value = '';
					ElMessage.warning('未检测到可用摄像头，请检查连接后刷新。');
				}
			} else {
				ElMessage.error(payload.message || '获取摄像头列表失败');
			}
		})
		.catch((error) => {
			console.error('获取摄像头列表失败:', error);
			ElMessage.error('获取摄像头列表失败，请检查 Flask 服务是否运行');
		})
		.finally(() => {
			state.cameraLoading = false;
		});
};


const start = () => {
	if (!kind.value) {
		ElMessage.warning('请先选择作物种类');
		return;
	}
	if (!weight.value) {
		ElMessage.warning('请先选择模型');
		return;
	}
	if (state.camera_items.length > 0 && cameraIndex.value === '') {
		ElMessage.warning('请先选择摄像头');
		return;
	}
	state.cameraisShow = true;
	state.form.weight = weight.value;
	state.form.kind = kind.value;
	state.form.conf = Number((conf.value / 100).toFixed(2));
	state.form.username = userInfos.value.userName;
	state.form.startTime = formatDate(new Date(), 'YYYY-mm-dd HH:MM:SS');
	state.form.cameraIndex = cameraIndex.value !== '' ? String(cameraIndex.value) : '';
	console.log(state.form);
	const params = new URLSearchParams();
	Object.entries(state.form).forEach(([key, value]) => {
		if (value !== '' && value !== null && value !== undefined) {
			params.append(key, String(value));
		}
	});
	const queryParams = params.toString();
	state.video_path = `http://127.0.0.1:5001/predictCamera?${queryParams}&t=${Date.now()}`;
};

const stop = () => {
	request.get('/flask/stopCamera').then((res) => {
		if (res.code == 0) {
			res.data = JSON.parse(res.data);
			console.log(res.data);
			state.weight_items = res.data.weight_items;
		} else {
			ElMessage.error(res.msg);
		}
	});
	state.cameraisShow = false
	state.video_path = ''
};

onMounted(() => {
	getData();
	fetchCameras();
});
</script>

<style scoped lang="scss">
.system-predict-container {
	width: 100%;
	height: 100%;
	display: flex;
	flex-direction: column;
	// background: radial-gradient(circle, #e3f7ef 0%, #ffffff 100%);

	.system-predict-padding {
		padding: 15px;
		background: radial-gradient(circle, #e3f7ef 0%, #ffffff 100%);

		.el-table {
			flex: 1;
		}
	}
}

.header {
	width: 100%;
	height: 5%;
	display: flex;
	justify-content: start;
	align-items: center;
	font-size: 20px;
}

.cards {
	width: 100%;
	height: 95%;
	border-radius: var(--next-radius-md);
	margin-top: 15px;
	padding: 0px;
	overflow: hidden;
	display: flex;
	justify-content: center;
	align-items: center;
	background: radial-gradient(circle, #e3f7ef 0%, #ffffff 100%);
	/* 防止视频溢出 */
}

.video {
	width: 100%;
	max-height: 100%;
	/* 限制视频最大高度不超过父元素高度 */
	height: auto;
	object-fit: contain;
}

.button-section {
	display: flex;
	justify-content: center;
}

.predict-button {
	width: 100%;
	/* 按钮宽度填满 */
}

.demo-progress .el-progress--line {
	margin-left: 20px;
	width: 600px;
}
</style>
