<template>
	<div class="chat-container">
		<el-card class="chat-card">
			<!-- 头部 -->
			<div class="chat-header">
				<div class="header-left">
					<i class="el-icon-chat-line-square"></i>
					<h2>智能问答助手</h2>
				</div>
				<div class="header-right">
					<el-button @click="clearHistory" class="clear-btn">
						<el-icon><Delete /></el-icon>
						<span>清空对话</span>
					</el-button>
				</div>
			</div>

			<!-- 消息列表 -->
			<div class="chat-messages" ref="messageList">
				<div v-if="messages.length === 0" class="welcome-message">
					<div class="welcome-icon">
						<i class="el-icon-chat-dot-round"></i>
					</div>
					<h3>欢迎使用智能问答助手</h3>
					<p>我可以帮您了解病害、防治方案、气象数据等相关问题</p>
					<div class="quick-questions">
						<el-tag
							v-for="(question, index) in quickQuestions"
							:key="index"
							class="quick-question-tag"
							@click="sendQuickQuestion(question)"
						>
							{{ question }}
						</el-tag>
					</div>
					<div class="history-section">
						<h4 class="history-title">最近常见提问</h4>
						<div class="history-grid">
							<div v-for="(sample, idx) in historySamples" :key="idx" class="history-card">
								<div class="history-card__header">
									<i class="el-icon-document"></i>
									<span>{{ sample.title }}</span>
								</div>
								<div class="history-card__thread">
									<div
										v-for="(turn, turnIdx) in sample.conversation"
										:key="turnIdx"
										class="history-card__turn"
										:class="{ 'is-user': turn.role === 'user' }"
									>
										<span class="label">{{ turn.role === 'user' ? '农户' : '助手' }}</span>
										<span class="text">{{ turn.content }}</span>
									</div>
								</div>
							</div>
						</div>
					</div>
				</div>

				<div
					v-for="(message, index) in messages"
					:key="index"
					class="message-item"
					:class="{ 'user-message': message.role === 'user', 'assistant-message': message.role === 'assistant' }"
				>
					<div class="message-avatar">
						<i v-if="message.role === 'user'" class="el-icon-user"></i>
						<i v-else class="el-icon-cpu"></i>
					</div>
					<div class="message-content">
						<div class="message-text" v-html="formatMessage(message.content)"></div>
						<div class="message-time">{{ message.time }}</div>
					</div>
				</div>

				<!-- 正在回复提示 -->
				<div v-if="isTyping" class="message-item assistant-message">
					<div class="message-avatar">
						<i class="el-icon-cpu"></i>
					</div>
					<div class="message-content">
						<div class="typing-indicator">
							<span></span>
							<span></span>
							<span></span>
						</div>
					</div>
				</div>
			</div>

			<!-- 输入区 -->
			<div class="chat-input">
				<el-input
					v-model="inputMessage"
					placeholder="请输入您的问题..."
					@keyup.enter.native="sendMessage"
					:disabled="isSending"
				>
					<template #append>
						<el-button @click="sendMessage" :loading="isSending" type="primary" class="send-btn">
							<el-icon><Promotion /></el-icon>
							<span>发送</span>
						</el-button>
					</template>
				</el-input>
			</div>
		</el-card>
	</div>
</template>

<script lang="ts">
import { defineComponent, nextTick } from 'vue';
import { ElMessage } from 'element-plus';
import { Delete, Promotion } from '@element-plus/icons-vue';
import request from '/@/utils/request';
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';

interface ChatMessage {
	role: 'user' | 'assistant';
	content: string;
	time: string;
}

interface StreamResponse {
	sessionId: string;
	content: string;
	isEnd: boolean;
	status: number;
	errorMessage?: string;
}

export default defineComponent({
	name: 'ChatView',
	components: {
		Delete,
		Promotion,
	},
	data() {
		return {
			messages: [] as ChatMessage[],
			inputMessage: '',
			isSending: false,
			isTyping: false,
			sessionId: '',
			stompClient: null as any,
			quickQuestions: [
				'如何防治玉米锈病？',
				'什么时间施药最合适？',
				'如何查看最新气象数据？',
				'连续降雨对施药有什么影响？',
			],
			historySamples: [
				{
					title: '玉米灰斑病复发处理',
					conversation: [
						{ role: 'user', content: '田里灰斑病反复出现，需要怎么加强防治？' },
						{
							role: 'assistant',
							content:
								'结合识别记录重点排查密植区域，轮换使用三唑酮与波尔多液，并减少氮肥追施，保持通风。',
						},
					],
				},
				{
					title: '连续阴雨的施药时机',
					conversation: [
						{ role: 'user', content: '最近阴雨不断，还能安排防治作业吗？' },
						{
							role: 'assistant',
							content:
								'关注平台气象监测，选择未来 4 小时内无降雨且风速低于 5m/s 的时段喷施，并同步增加保温除湿。',
						},
					],
				},
				{
					title: '无人机巡检后的处置',
					conversation: [
						{ role: 'user', content: '无人机巡检后需要做哪些整理工作？' },
						{
							role: 'assistant',
							content:
								'请上传高清图像、标记积水和病斑区域，并在农事计划里创建后续整改任务以便跟踪执行。',
						},
					],
				},
				{
					title: '水稻纹枯病预警',
					conversation: [
						{ role: 'user', content: '预警说纹枯病风险升高，需要马上处理吗？' },
						{
							role: 'assistant',
							content:
								'请优先巡查低洼及密植区，按推荐方案配置井冈·蜡芽菌或噻呋酰胺，并控制氮肥用量，保持通风。',
						},
					],
				},
				{
					title: '草莓采前管理建议',
					conversation: [
						{ role: 'user', content: '草莓采收前需要注意什么？' },
						{
							role: 'assistant',
							content:
								'建议提前 7 天停止高浓度药剂喷施，保持棚内通风干燥，并根据检测结果适量补钙提升抗压能力。',
						},
					],
				},
				{
					title: '传感器数据波动',
					conversation: [
						{ role: 'user', content: '传感器湿度值突然升高，是设备问题吗？' },
						{
							role: 'assistant',
							content:
								'请先核对最新气象数据和灌溉记录，确认无异常后，可在设备中心执行远程重启并检查电量与信号强度。',
						},
					],
				},
				{
					title: '番茄温室夜间管理',
					conversation: [
						{ role: 'user', content: '夜间温室湿度高，番茄叶片出现水渍斑怎么办？' },
						{
							role: 'assistant',
							content:
								'建议开启循环风和除湿机，将湿度控制在 80% 以下，并在清晨通风后补喷含铜药剂预防晚疫病。',
						},
					],
				},
				{
					title: '农事任务派工疑问',
					conversation: [
						{ role: 'user', content: '任务已经生成，如何批量指派执行人？' },
						{
							role: 'assistant',
							content:
								'可在农事计划列表中勾选多条任务，点击“派发”后选择执行人并设定状态，系统会同步通知到移动端。',
						},
					],
				},
				{
					title: '灌溉水质监测',
					conversation: [
						{ role: 'user', content: '灌溉水电导率偏高，会影响水稻吗？' },
						{
							role: 'assistant',
							content:
								'若 EC 值持续高于 1.2 mS/cm，需适量稀释或更换水源，并记录在水质监测页，必要时同步调整施肥方案。',
						},
					],
				},
				{
					title: '病虫害图像上传规范',
					conversation: [
						{ role: 'user', content: '上传的叶片照片老是识别失败，要怎么拍？' },
						{
							role: 'assistant',
							content:
								'请保持光线均匀、对焦清晰，尽量将病斑占画面 60%以上，避免背光和水滴反光，支持 jpg/png 格式。',
						},
					],
				},
			],
		};
	},
	mounted() {
		this.sessionId = this.generateSessionId();
		this.loadHistory();
		this.connectWebSocket();
	},
	beforeUnmount() {
		this.disconnectWebSocket();
	},
	methods: {
		isSuccess(code: unknown) {
			return code === 0 || code === '0';
		},
		generateSessionId() {
			return `session-${Date.now()}-${Math.random().toString(16).slice(2)}`;
		},
		scrollToBottom() {
			nextTick(() => {
				const list = this.$refs.messageList as HTMLElement | undefined;
				if (list) {
					list.scrollTop = list.scrollHeight;
				}
			});
		},
		formatMessage(content: string) {
			return content.replace(/\n/g, '<br />');
		},
		appendMessage(role: ChatMessage['role'], content: string) {
			const now = new Date();
			this.messages.push({
				role,
				content,
				time: now.toLocaleString(),
			});
			this.scrollToBottom();
		},
		handleStreamMessage(frame: any) {
			try {
				const payload: StreamResponse = JSON.parse(frame.body);
				if (payload.status !== 0 && payload.isEnd) {
					this.isTyping = false;
					if (payload.errorMessage) {
						this.updateAssistantMessage(payload.errorMessage, true);
					}
					return;
				}

				if (!payload.isEnd) {
					this.updateAssistantMessage(payload.content);
				} else {
					this.isTyping = false;
				}
			} catch (error) {
				console.error('解析消息失败', error);
				this.isTyping = false;
				this.updateAssistantMessage('对话发生异常，请稍后再试。', true);
			}
		},
		updateAssistantMessage(content: string, forceReplace = false) {
			const lastMessage = this.messages[this.messages.length - 1];
			if (!lastMessage || lastMessage.role !== 'assistant') {
				this.appendMessage('assistant', content);
				return;
			}
			lastMessage.content = forceReplace ? content : lastMessage.content + content;
			this.scrollToBottom();
		},
		connectWebSocket() {
			const socket = new SockJS('/api/chat/ws');
			this.stompClient = Stomp.over(socket);
			this.stompClient.debug = () => {};
			this.stompClient.connect(
				{},
				() => {
					this.stompClient.subscribe(`/topic/chat/${this.sessionId}`, this.handleStreamMessage);
				},
				(error: any) => {
					console.error('连接 WebSocket 失败', error);
					ElMessage.error('连接聊天服务失败，请稍后再试');
				},
			);
		},
		disconnectWebSocket() {
			if (this.stompClient && this.stompClient.connected) {
				this.stompClient.disconnect();
			}
		},
		sendMessage() {
			const message = this.inputMessage.trim();
			if (!message) {
				return;
			}

			if (!this.stompClient || !this.stompClient.connected) {
				ElMessage.warning('聊天服务未连接，请稍后重试');
				return;
			}

			this.appendMessage('user', message);
			this.inputMessage = '';
			this.isTyping = true;
			this.isSending = true;

			// 准备助手消息占位
			this.appendMessage('assistant', '');

			const payload = {
				sessionId: this.sessionId,
				message,
			};

			this.stompClient.send('/app/chat/stream', {}, JSON.stringify(payload));

			setTimeout(() => {
				this.isSending = false;
			}, 200);
		},
		sendQuickQuestion(question: string) {
			this.inputMessage = question.replace(/[？?]$/, '');
			this.sendMessage();
		},
		loadHistory() {
			request
				.get(`/api/chat/history/${this.sessionId}`)
				.then((res) => {
					if (this.isSuccess(res.code) && Array.isArray(res.data)) {
						this.messages = res.data.map((item: any) => ({
							role: item.role === 'assistant' ? 'assistant' : 'user',
							content: item.content,
							time: new Date().toLocaleString(),
						}));
						this.scrollToBottom();
					}
				})
				.catch((error) => {
					console.error('获取历史失败', error);
				});
		},
		clearHistory() {
			request
				.delete(`/api/chat/history/${this.sessionId}`)
				.then((res) => {
					if (this.isSuccess(res.code)) {
						this.messages = [];
						ElMessage.success('对话已清空');
					} else {
						ElMessage.error(res.msg || '清空对话失败');
					}
				})
				.catch((error) => {
					console.error('清空历史失败', error);
					ElMessage.error('清空对话失败，请稍后再试');
				});
		},
	},
});
</script>

<style scoped lang="scss">
.chat-container {
	display: flex;
	justify-content: center;
	background: linear-gradient(135deg, #e8f5e9 0%, #f1f8ff 100%);
	min-height: 100%;
	padding: 0 12px 32px;
}

.chat-card {
	width: min(98%, 1620px);
	border-radius: var(--next-radius-md);
	margin: 0 auto;
	padding: 36px 44px;
}

.chat-header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 16px;
}

.header-left {
	display: flex;
	align-items: center;
	gap: 8px;
	color: #2f8c56;
}

.header-left h2 {
	margin: 0;
	font-size: 20px;
	font-weight: 600;
}

.header-right {
	display: flex;
	align-items: center;
}

.header-right :deep(.el-button) {
	display: inline-flex;
	align-items: center;
	justify-content: center;
	height: 36px;
	padding: 0 20px;
	font-size: 14px;
}

:deep(.clear-btn),
:deep(.send-btn) {
	display: inline-flex;
	align-items: center;
	gap: 6px;
}

:deep(.clear-btn .el-icon),
:deep(.send-btn .el-icon) {
	margin-right: 2px;
	font-size: 16px;
}

.chat-messages {
	min-height: 540px;
	max-height: 700px;
	overflow-y: auto;
	padding: 24px 28px;
	background: #f9fbff;
	border-radius: var(--next-radius-lg);
	margin-bottom: 20px;
}

.welcome-message {
	text-align: center;
	color: #4f6b7a;
}

.welcome-icon {
	font-size: 48px;
	color: #3aa372;
	margin-bottom: 12px;
}

.quick-questions {
	display: flex;
	justify-content: center;
	flex-wrap: wrap;
	gap: 10px;
	margin-top: 16px;
}

.quick-question-tag {
	cursor: pointer;
}

.history-section {
	margin-top: 24px;
	text-align: left;
}
.history-title {
	margin-bottom: 12px;
	font-size: 15px;
	font-weight: 600;
	color: #2f6fa6;
}
.history-grid {
	display: grid;
	grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
	gap: 20px;
}
.history-card {
	background: #ffffff;
	border-radius: 12px;
	box-shadow: 0 6px 16px rgba(47, 140, 86, 0.08);
	padding: 14px 16px;
	display: flex;
	flex-direction: column;
	gap: 10px;
}
.history-card__header {
	display: flex;
	align-items: center;
	gap: 6px;
	font-weight: 600;
	color: #1f845a;
}
.history-card__thread {
	display: flex;
	flex-direction: column;
	gap: 8px;
}
.history-card__turn {
	display: flex;
	gap: 8px;
	align-items: flex-start;
	color: #4f6b7a;
}
.history-card__turn.is-user .label {
	background: #e6f0ff;
	color: #2f6fa6;
}
.history-card__turn .label {
	padding: 2px 6px;
	border-radius: 6px;
	font-size: 12px;
	background: #eaf7f0;
	color: #2f8c56;
}
.history-card__turn .text {
	flex: 1;
	font-size: 13px;
	line-height: 1.5;
}

.message-item {
	display: flex;
	gap: 12px;
	margin-bottom: 18px;
}

.message-item:last-child {
	margin-bottom: 0;
}

.message-avatar {
	width: 36px;
	height: 36px;
	border-radius: 50%;
	display: flex;
	justify-content: center;
	align-items: center;
	background: #dcedf5;
	color: #2f6fa6;
	font-size: 18px;
}

.assistant-message .message-avatar {
	background: #e6f4ea;
	color: #2f8c56;
}

.message-content {
	max-width: calc(100% - 60px);
	background: #fff;
	border-radius: var(--next-radius-lg);
	padding: 12px 16px;
	box-shadow: 0 1px 4px rgba(15, 36, 70, 0.08);
}

.user-message .message-content {
	background: #e6f0ff;
}

.message-text {
	color: #1f2d3d;
	line-height: 1.6;
	word-break: break-word;
	white-space: pre-wrap;
}

.message-time {
	margin-top: 6px;
	font-size: 12px;
	color: #8b9cab;
	text-align: right;
}

.typing-indicator {
	display: flex;
	gap: 6px;
}

.typing-indicator span {
	display: block;
	width: 8px;
	height: 8px;
	background: #3aa372;
	border-radius: 50%;
	animation: typing 1.2s infinite ease-in-out;
}

.typing-indicator span:nth-child(2) {
	animation-delay: 0.2s;
}

.typing-indicator span:nth-child(3) {
	animation-delay: 0.4s;
}

@keyframes typing {
	0%,
	80%,
	100% {
		opacity: 0.2;
		transform: translateY(0);
	}
	40% {
		opacity: 1;
		transform: translateY(-6px);
	}
}

.chat-input .el-input {
	font-size: 14px;
}
</style>
