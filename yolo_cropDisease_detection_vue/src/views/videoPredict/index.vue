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
				<el-upload v-model="state.form.inputVideo" ref="uploadFile" class="avatar-uploader"
					action="http://localhost:9999/files/upload" :show-file-list="false"
					:on-success="handleAvatarSuccessone">
					<div class="button-section" style="margin-left: 20px">
						<el-button type="info" class="predict-button">上传视频</el-button>
					</div>
				</el-upload>
				<div class="button-section" style="margin-left: 20px">
					<el-button type="primary" @click="upData" class="predict-button">开始处理</el-button>
				</div>
				<div class="demo-progress" v-if="state.isShow">
					<el-progress :text-inside="true" :stroke-width="20" :percentage=state.percentage style="width: 380px;">
						<span>{{ state.type_text }} {{ state.percentage }}%</span>
					</el-progress>
				</div>
			</div>
			<div class="cards" ref="cardsContainer">
				<img v-if="state.video_path" class="video" :src="state.video_path">
			</div>
		</div>
	</div>
</template>


<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import type { MessageHandler } from 'element-plus';
import request from '/@/utils/request';
import { useUserInfo } from '/@/stores/userInfo';
import { storeToRefs } from 'pinia';
import type { UploadInstance, UploadProps } from 'element-plus';
import { SocketService } from '/@/utils/socket';
import { formatDate } from '/@/utils/formatTime';

const uploadFile = ref<UploadInstance>();
const stores = useUserInfo();
const conf = ref(50);
const kind = ref('');
const weight = ref('');
const { userInfos } = storeToRefs(stores);

const handleAvatarSuccessone: UploadProps['onSuccess'] = (response, uploadFile) => {
	ElMessage.success('上传成功！');
	state.form.inputVideo = response.data;
};
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
	],
	data: {} as any,
	video_path: '',
	type_text: "正在保存",
	percentage: 50,
	isShow: false,
	form: {
		username: '',
		inputVideo: null as any,
		weight: '',
		conf: null as any,
		kind: '',
		startTime: ''
	},
});

const socketService = SocketService.getInstance();
let socketMessageHandler: MessageHandler | null = null;

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
		if (socketMessageHandler) {
			socketMessageHandler.close();
		}
		socketMessageHandler = ElMessage.success(data);
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


const upData = () => {
	state.form.weight = weight.value;
	state.form.conf = Number((conf.value / 100).toFixed(2));
	state.form.username = userInfos.value.userName;
	state.form.kind = kind.value;
	state.form.startTime = formatDate(new Date(), 'YYYY-mm-dd HH:MM:SS');
	console.log(state.form);
	const queryParams = new URLSearchParams(state.form).toString();
	state.video_path = `http://127.0.0.1:5001/predictVideo?${queryParams}`;
	ElMessage.success('正在加载！');
};

onMounted(() => {
	getData();
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
