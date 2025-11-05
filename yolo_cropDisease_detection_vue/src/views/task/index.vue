<template>
	<div class="task-page layout-padding">
		<div class="task-wrapper layout-padding-auto layout-padding-view">
			<div class="task-header">
				<div class="title-block">
					<h2>农事计划</h2>
					<p>制定、派发与追踪农事任务，实时掌握执行进度。</p>
				</div>
				<el-button type="primary" size="large" @click="openCreateDialog">
					<el-icon><EleCirclePlus /></el-icon>
					新增任务
				</el-button>
			</div>
			<el-row :gutter="16" class="status-cards">
				<el-col v-for="item in statusCards" :key="item.status" :md="6" :sm="12" :xs="24">
					<div class="status-card" :class="item.status">
						<div class="status-card__label">{{ item.label }}</div>
						<div class="status-card__value">{{ item.value }}</div>
						<p class="status-card__desc">{{ item.desc }}</p>
					</div>
				</el-col>
			</el-row>
			<el-card class="task-card" shadow="never">
				<div class="task-filter">
					<el-select v-model="filters.status" placeholder="状态筛选" clearable @change="handleFilterChange">
						<el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
					</el-select>
					<el-select v-model="filters.taskType" placeholder="任务类型" clearable @change="handleFilterChange">
						<el-option v-for="item in taskTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
					</el-select>
					<el-select v-model="filters.executorId" placeholder="执行人" clearable filterable @change="handleFilterChange">
						<el-option v-for="item in executorOptions" :key="item.id" :label="item.name" :value="item.id" />
					</el-select>
					<el-input v-model="filters.keyword" placeholder="按地块/说明模糊搜索" clearable @keyup.enter="handleFilterChange" />
				</div>
				<el-table :data="tableData" border stripe v-loading="loading" class="task-table">
					<el-table-column prop="id" label="任务编号" width="110" align="center" />
					<el-table-column prop="fieldId" label="地块" width="120" align="center">
						<template #default="{ row }">
							<el-tag effect="light">{{ formatField(row.fieldId) }}</el-tag>
						</template>
					</el-table-column>
					<el-table-column prop="taskType" label="任务类型" width="140" align="center">
						<template #default="{ row }">
							<el-tag :type="taskTypeStyle[row.taskType]?.tagType || 'info'">
								{{ formatTaskType(row.taskType) }}
							</el-tag>
						</template>
					</el-table-column>
					<el-table-column prop="planWindow" label="计划窗口" min-width="220">
						<template #default="{ row }">
							<div class="plan-window">
								<div class="plan-window__main">{{ row.planWindow.label }}</div>
								<div v-if="row.planWindow.subLabel" class="plan-window__sub">{{ row.planWindow.subLabel }}</div>
							</div>
						</template>
					</el-table-column>
					<el-table-column prop="executorName" label="执行人" width="120" align="center">
						<template #default="{ row }">
							<span>{{ row.executorName || '待指派' }}</span>
						</template>
					</el-table-column>
					<el-table-column prop="status" label="当前状态" width="120" align="center">
						<template #default="{ row }">
							<el-tag :type="statusMap[row.status]?.tagType || 'info'">
								{{ statusMap[row.status]?.label || row.status }}
							</el-tag>
						</template>
					</el-table-column>
					<el-table-column prop="resourceUsageDisplay" label="农资消耗" min-width="200">
						<template #default="{ row }">
							<div class="resource-tags" v-if="row.resourceUsageDisplay.length">
								<el-tag v-for="item in row.resourceUsageDisplay" :key="item.key" size="small">{{ item.key }}：{{ item.value }}</el-tag>
							</div>
							<span v-else>—</span>
						</template>
					</el-table-column>
					<el-table-column prop="description" label="任务说明" min-width="200" show-overflow-tooltip />
					<el-table-column label="操作" fixed="right" width="220" align="center">
						<template #default="{ row }">
							<el-space wrap>
								<el-button v-if="canAssign(row.status)" size="small" type="primary" plain @click="openAssignDialog(row)">派发</el-button>
								<el-button v-if="row.status === 'assigned'" size="small" type="success" plain @click="markInProgress(row)">开始执行</el-button>
								<el-button v-if="row.status === 'in_progress'" size="small" type="success" @click="markCompleted(row)">标记完成</el-button>
								<el-popconfirm title="确定要删除该任务吗？" @confirm="deleteTask(row.id)">
									<template #reference>
										<el-button size="small" type="danger" plain>删除</el-button>
									</template>
								</el-popconfirm>
							</el-space>
						</template>
					</el-table-column>
				</el-table>
				<el-pagination
					class="task-pagination"
					background
					layout="total, sizes, prev, pager, next, jumper"
					:current-page="pagination.pageNum"
					:page-size="pagination.pageSize"
					:total="pagination.total"
					:page-sizes="[10, 20, 30]"
					@current-change="handlePageChange"
					@size-change="handleSizeChange"
				/>
			</el-card>
		</div>

		<el-dialog v-model="createDialog.visible" title="新增农事任务" width="520px" destroy-on-close>
			<el-form :model="createDialog.form" :rules="createRules" ref="createFormRef" label-width="100px">
				<el-form-item label="地块编号" prop="fieldId">
					<el-input v-model="createDialog.form.fieldId" placeholder="如 101 表示一号玉米田" />
				</el-form-item>
				<el-form-item label="任务类型" prop="taskType">
					<el-select v-model="createDialog.form.taskType" placeholder="请选择任务类型">
						<el-option v-for="item in taskTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
					</el-select>
				</el-form-item>
				<el-form-item label="计划窗口" prop="planTime">
					<el-date-picker
						v-model="createDialog.form.planTime"
						type="datetimerange"
						value-format="YYYY-MM-DD HH:mm:ss"
						range-separator="至"
						start-placeholder="开始时间"
						end-placeholder="结束时间"
					/>
				</el-form-item>
				<el-form-item label="执行人">
					<el-select v-model="createDialog.form.executorId" placeholder="可选，支持后续派发" clearable>
						<el-option v-for="item in executorOptions" :key="item.id" :label="item.name" :value="item.id" />
					</el-select>
				</el-form-item>
				<el-form-item label="农资消耗">
					<el-input
						v-model="createDialog.form.resourceUsage"
						type="textarea"
						placeholder='以 JSON 形式填写，例如 {"fertilizer":"复合肥 20kg"}'
						:rows="3"
					/>
				</el-form-item>
				<el-form-item label="任务说明">
					<el-input v-model="createDialog.form.description" type="textarea" :rows="3" placeholder="补充任务细节、注意事项等" />
				</el-form-item>
			</el-form>
			<template #footer>
				<el-button @click="createDialog.visible = false">取消</el-button>
				<el-button type="primary" @click="submitCreate">保存</el-button>
			</template>
		</el-dialog>

		<el-dialog v-model="assignDialog.visible" title="派发任务" width="420px" destroy-on-close>
			<el-form :model="assignDialog.form" label-width="90px">
				<el-form-item label="任务编号">
					<span>{{ assignDialog.form.id }}</span>
				</el-form-item>
				<el-form-item label="执行人" prop="executorId">
					<el-select v-model="assignDialog.form.executorId" placeholder="请选择执行人">
						<el-option v-for="item in executorOptions" :key="item.id" :label="item.name" :value="item.id" />
					</el-select>
				</el-form-item>
				<el-form-item label="状态调整">
					<el-select v-model="assignDialog.form.status">
						<el-option label="派发中" value="assigned" />
						<el-option label="立即执行" value="in_progress" />
					</el-select>
				</el-form-item>
			</el-form>
			<template #footer>
				<el-button @click="assignDialog.visible = false">取消</el-button>
				<el-button type="primary" @click="submitAssign">确认派发</el-button>
			</template>
		</el-dialog>
	</div>
</template>

<script setup lang="ts" name="taskPlan">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElNotification, FormInstance, FormRules } from 'element-plus';
import { CirclePlus as EleCirclePlus } from '@element-plus/icons-vue';
import { fetchTaskPage, fetchTaskStats, createTask, assignTask, updateTaskStatus, removeTask } from '/@/api/task/index';
import request from '/@/utils/request';
import { SocketService } from '/@/utils/socket';

interface TaskPlanWindow {
	label: string;
	subLabel?: string;
}

interface TaskRow {
	id: number;
	fieldId: number;
	taskType: string;
	planStartTime: string | null;
	planEndTime: string | null;
	executorId: number | null;
	status: string;
	resourceUsage: string | null;
	description: string | null;
	progressUpdatedAt: string | null;
	executorName?: string | null;
	planWindow: TaskPlanWindow;
	resourceUsageDisplay: Array<{ key: string; value: string }>;
}

const socketService = SocketService.getInstance();
const loading = ref(false);
const tableData = ref<TaskRow[]>([]);
const pagination = reactive({
	pageNum: 1,
	pageSize: 10,
	total: 0,
});

const filters = reactive({
	status: '',
	taskType: '',
	executorId: null as number | null,
	keyword: '',
});

const statusMap: Record<string, { label: string; tagType: 'success' | 'info' | 'warning' | 'danger' | 'primary' }> = {
	planned: { label: '已计划', tagType: 'info' },
	assigned: { label: '已派发', tagType: 'primary' },
	in_progress: { label: '执行中', tagType: 'warning' },
	completed: { label: '已完成', tagType: 'success' },
	failed: { label: '异常终止', tagType: 'danger' },
};

const statusOptions = [
	{ label: '全部状态', value: '' },
	{ label: '已计划', value: 'planned' },
	{ label: '已派发', value: 'assigned' },
	{ label: '执行中', value: 'in_progress' },
	{ label: '已完成', value: 'completed' },
	{ label: '异常终止', value: 'failed' },
];

const taskTypeOptions = [
	{ label: '巡检', value: 'patrol' },
	{ label: '田间巡查', value: 'inspection' },
	{ label: '病害复查', value: 'disease_followup' },
	{ label: '施肥', value: 'fertilization' },
	{ label: '灌溉', value: 'irrigation' },
	{ label: '植保', value: 'protection' },
	{ label: '方案执行', value: 'solution_application' },
	{ label: '虫情监测', value: 'pest_monitor' },
	{ label: '无人机巡查', value: 'drone_scouting' },
	{ label: '采前预检', value: 'harvest_precheck' },
	{ label: '土壤采样', value: 'soil_sampling' },
	{ label: '设备维护', value: 'equipment_maintenance' },
	{ label: '采收', value: 'harvest' },
	{ label: '其他', value: 'others' },
];

const taskTypeStyle: Record<string, { label: string; tagType: 'success' | 'info' | 'warning' | 'danger' | 'primary' }> = {
	patrol: { label: '巡检', tagType: 'primary' },
	inspection: { label: '田间巡查', tagType: 'primary' },
	disease_followup: { label: '病害复查', tagType: 'warning' },
	fertilization: { label: '施肥', tagType: 'success' },
	irrigation: { label: '灌溉', tagType: 'info' },
	protection: { label: '植保', tagType: 'warning' },
	solution_application: { label: '方案执行', tagType: 'warning' },
	pest_monitor: { label: '虫情监测', tagType: 'danger' },
	drone_scouting: { label: '无人机巡查', tagType: 'info' },
	harvest_precheck: { label: '采前预检', tagType: 'success' },
	soil_sampling: { label: '土壤采样', tagType: 'primary' },
	equipment_maintenance: { label: '设备维护', tagType: 'info' },
	harvest: { label: '采收', tagType: 'danger' },
	others: { label: '其他', tagType: 'info' },
};

const executorOptions = ref<Array<{ id: number; name: string }>>([]);
const stats = reactive({
	planned: 0,
	assigned: 0,
	in_progress: 0,
	completed: 0,
	failed: 0,
});

const statusCards = computed(() => [
	{ status: 'planned', label: '已计划', value: stats.planned, desc: '等待派发或执行' },
	{ status: 'assigned', label: '已派发', value: stats.assigned, desc: '已安排执行人' },
	{ status: 'in_progress', label: '执行中', value: stats.in_progress, desc: '实时关注执行进度' },
	{ status: 'completed', label: '已完成', value: stats.completed, desc: '执行结束待复盘' },
]);

const createDialog = reactive({
	visible: false,
	form: {
		fieldId: '',
		taskType: '',
		planTime: [] as string[] | [],
		executorId: null as number | null,
		resourceUsage: '',
		description: '',
	},
});
const createFormRef = ref<FormInstance>();
const createRules: FormRules = {
	fieldId: [{ required: true, message: '请输入地块编号', trigger: 'blur' }],
	taskType: [{ required: true, message: '请选择任务类型', trigger: 'change' }],
	planTime: [{ required: true, message: '请选择计划时间', trigger: 'change' }],
};

const assignDialog = reactive({
	visible: false,
	form: {
		id: 0,
		executorId: null as number | null,
		status: 'assigned',
	},
});

const fetchExecutors = async () => {
	const res = await request.get('/api/user/all');
	if (res.code === 0) {
		executorOptions.value = res.data.map((item: any) => ({ id: item.id, name: item.name || item.username }));
	}
};

const transformResourceUsage = (raw: string | null) => {
	if (!raw) return [];
	try {
		const parsed = JSON.parse(raw);
		if (Array.isArray(parsed)) return parsed;
		if (typeof parsed === 'object') {
			return Object.entries(parsed).map(([key, value]) => ({ key, value: String(value) }));
		}
		return [];
	} catch (error) {
		return [];
	}
};

const fetchStatsData = async () => {
	const res = await fetchTaskStats();
	if (res.code === 0) {
		stats.planned = res.data.planned || 0;
		stats.assigned = res.data.assigned || 0;
		stats.in_progress = res.data.in_progress || 0;
		stats.completed = res.data.completed || 0;
		stats.failed = res.data.failed || 0;
	}
};

const fetchTableData = async () => {
	loading.value = true;
	try {
		const params: Record<string, any> = {
			pageNum: pagination.pageNum,
			pageSize: pagination.pageSize,
		};
		if (filters.status) params.status = filters.status;
		if (filters.taskType) params.taskType = filters.taskType;
		if (filters.executorId) params.executorId = filters.executorId;
		if (filters.keyword) params.keyword = filters.keyword;
		const res = await fetchTaskPage(params);
		if (res.code === 0) {
			pagination.total = res.data.total;
			tableData.value = res.data.records.map((item: any) => {
				const row: TaskRow = {
					id: item.id,
					fieldId: item.fieldId,
					taskType: item.taskType,
					planStartTime: item.planStartTime,
					planEndTime: item.planEndTime,
					executorId: item.executorId,
					status: item.status,
					resourceUsage: item.resourceUsage,
					description: item.description,
					progressUpdatedAt: item.progressUpdatedAt,
					executorName: resolveExecutorName(item.executorId),
					planWindow: formatPlanWindow(item.planStartTime, item.planEndTime),
					resourceUsageDisplay: transformResourceUsage(item.resourceUsage),
				};
				return row;
			});
		}
	} finally {
		loading.value = false;
	}
};

const resolveExecutorName = (executorId: number | null) => {
	if (!executorId) return null;
	const target = executorOptions.value.find((user) => user.id === executorId);
	return target ? target.name : `工号 ${executorId}`;
};

const formatPlanWindow = (start?: string, end?: string): TaskPlanWindow => {
	if (!start && !end) return { label: '—' };

	const format = (value: string) => {
		const [date, time = ''] = value.split(' ');
		try {
			const dateObj = new Date(value.replace(/-/g, '/'));
			const day = `${dateObj.getMonth() + 1}月${dateObj.getDate()}日`;
			const hour = `${dateObj.getHours().toString().padStart(2, '0')}:${dateObj.getMinutes().toString().padStart(2, '0')}`;
			return { date, day, hour };
		} catch (error) {
			return { date, day: date, hour: time || '--:--' };
		}
	};

	if (!start || !end) {
		const target = format(start || end!);
		return {
			label: `${target.day} ${target.hour}`,
			subLabel: target.date,
		};
	}

	const startInfo = format(start);
	const endInfo = format(end);
	if (startInfo.date === endInfo.date) {
		return {
			label: `${startInfo.day} ${startInfo.hour} ~ ${endInfo.hour}`,
			subLabel: startInfo.date,
		};
	}

	return {
		label: `${startInfo.day} ${startInfo.hour} ~ ${endInfo.day} ${endInfo.hour}`,
		subLabel: `${startInfo.date} → ${endInfo.date}`,
	};
};

const formatField = (fieldId: number) => `地块 #${fieldId}`;

const formatTaskType = (taskType: string) => {
	const mapped = taskTypeStyle[taskType]?.label;
	if (mapped) return mapped;
	return taskType
		.split('_')
		.filter(Boolean)
		.map((part) => part.charAt(0).toUpperCase() + part.slice(1))
		.join(' ');
};

const handleFilterChange = () => {
	pagination.pageNum = 1;
	fetchTableData();
	fetchStatsData();
};

const handlePageChange = (page: number) => {
	pagination.pageNum = page;
	fetchTableData();
};

const handleSizeChange = (size: number) => {
	pagination.pageSize = size;
	fetchTableData();
};

const openCreateDialog = () => {
	createDialog.visible = true;
	createDialog.form = {
		fieldId: '',
		taskType: '',
		planTime: [],
		executorId: null,
		resourceUsage: '',
		description: '',
	};
};

const submitCreate = () => {
	if (!createFormRef.value) return;
	createFormRef.value.validate(async (valid) => {
		if (!valid) return;
		const payload: Record<string, any> = {
			fieldId: Number(createDialog.form.fieldId),
			taskType: createDialog.form.taskType,
			executorId: createDialog.form.executorId,
			description: createDialog.form.description,
			resourceUsage: createDialog.form.resourceUsage || null,
		};
		if (Array.isArray(createDialog.form.planTime) && createDialog.form.planTime.length === 2) {
			payload.planStartTime = createDialog.form.planTime[0];
			payload.planEndTime = createDialog.form.planTime[1];
		}
		const res = await createTask(payload);
		if (res.code === 0) {
			ElMessage.success('任务创建成功');
			createDialog.visible = false;
			fetchTableData();
			fetchStatsData();
		} else {
			ElMessage.error(res.msg || '任务创建失败');
		}
	});
};

const canAssign = (status: string) => ['planned', 'assigned'].includes(status);

const openAssignDialog = (row: TaskRow) => {
	assignDialog.visible = true;
	assignDialog.form = {
		id: row.id,
		executorId: row.executorId || null,
		status: row.status === 'in_progress' ? 'in_progress' : 'assigned',
	};
};

const submitAssign = async () => {
	if (!assignDialog.form.executorId) {
		ElMessage.warning('请先选择执行人');
		return;
	}
	const res = await assignTask(assignDialog.form.id, {
		executorId: assignDialog.form.executorId,
		status: assignDialog.form.status,
	});
	if (res.code === 0) {
		ElMessage.success('任务派发成功');
		assignDialog.visible = false;
		fetchTableData();
		fetchStatsData();
	} else {
		ElMessage.error(res.msg || '任务派发失败');
	}
};

const markInProgress = async (row: TaskRow) => {
	const res = await updateTaskStatus(row.id, { status: 'in_progress' });
	if (res.code === 0) {
		ElMessage.success('任务已置为执行中');
		fetchTableData();
		fetchStatsData();
	}
};

const markCompleted = async (row: TaskRow) => {
	const res = await updateTaskStatus(row.id, { status: 'completed' });
	if (res.code === 0) {
		ElMessage.success('任务已完成');
		fetchTableData();
		fetchStatsData();
	}
};

const deleteTask = async (id: number) => {
	const res = await removeTask(id);
	if (res.code === 0) {
		ElMessage.success('任务已删除');
		fetchTableData();
		fetchStatsData();
	}
};

const applySocketUpdate = (payload: any) => {
	if (!payload || !payload.taskId) return;
	const taskId = Number(payload.taskId);
	const target = tableData.value.find((item) => item.id === taskId);
	if (!target) return;
	if (payload.status) {
		target.status = payload.status;
	}
	if (payload.outImg) {
		target.description = `${target.description || ''} 识别结果: ${payload.outImg}`.trim();
	}
	if (payload.outVideo) {
		target.description = `${target.description || ''} 视频结果: ${payload.outVideo}`.trim();
	}
	if (payload.labels) {
		const labels = Array.isArray(payload.labels) ? payload.labels.join('、') : payload.labels;
		target.description = `${target.description || ''} 检测: ${labels}`.trim();
	}
	if (payload.allTime) {
		target.progressUpdatedAt = new Date().toISOString();
	}
	ElNotification({
		title: `任务 #${taskId} 更新`,
		message: statusMap[target.status]?.label || payload.status,
		type: payload.status === 'failed' ? 'error' : 'success',
		duration: 3000,
	});
	fetchStatsData();
};

onMounted(async () => {
	await fetchExecutors();
	await fetchTableData();
	await fetchStatsData();
	socketService.on('task_progress', applySocketUpdate);
});

onBeforeUnmount(() => {
	socketService.disconnect();
});
</script>

<style scoped lang="scss">
.task-page {
	display: flex;
	flex-direction: column;
	gap: 16px;
}

.task-wrapper {
	display: flex;
	flex-direction: column;
	gap: 20px;
}

.task-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	background: linear-gradient(120deg, rgba(33, 194, 148, 0.18), rgba(255, 255, 255, 0.6));
	border-radius: var(--next-radius-md);
	padding: 20px 28px;
	box-shadow: 0 10px 30px -20px rgba(33, 194, 148, 0.6);

	.title-block {
		display: flex;
		flex-direction: column;
		gap: 6px;
		h2 {
			margin: 0;
			font-size: 22px;
			color: #084c42;
		}
		p {
			margin: 0;
			color: #4f6f68;
		}
	}
}

.status-cards {
	.status-card {
		display: flex;
		flex-direction: column;
		gap: 6px;
		background: #ffffff;
		padding: 18px;
		border-radius: var(--next-radius-lg);
		box-shadow: 0 12px 35px -28px rgba(12, 111, 96, 0.6);
		border: 1px solid rgba(13, 117, 93, 0.12);
		&__label {
			font-size: 14px;
			color: #4d665f;
		}
		&__value {
			font-size: 28px;
			font-weight: 600;
			color: #0f7c63;
		}
		&__desc {
			font-size: 12px;
			color: #6b8a82;
		}
		&.in_progress .status-card__value {
			color: #f29718;
		}
		&.completed .status-card__value {
			color: #1a9d65;
		}
	}
}

.task-card {
	border: none;
	.task-filter {
		display: flex;
		flex-wrap: wrap;
		gap: 12px;
		margin-bottom: 16px;
		.el-select,
		.el-input {
			width: 200px;
		}
	}
}

.task-table {
	width: 100%;
}
:deep(.task-table .el-table__header-wrapper th) {
	background: #e8f7f1 !important;
}
:deep(.task-table .el-table__fixed-right),
:deep(.task-table .el-table__fixed) {
	background-color: #ffffff;
	z-index: 25 !important;
}
:deep(.task-table .el-table__fixed-right .el-table__cell),
:deep(.task-table .el-table__fixed .el-table__cell) {
	background-color: #ffffff !important;
}
:deep(.task-table .el-table__fixed-right .el-table__header-wrapper th),
:deep(.task-table .el-table__fixed .el-table__header-wrapper th) {
	background-color: #e8f7f1 !important;
}
:deep(.task-table .el-table__fixed-right::before),
:deep(.task-table .el-table__fixed::before) {
	display: none;
}
:deep(.task-table .el-table__fixed-right) {
	box-shadow: -10px 0 24px -20px rgba(14, 89, 67, 0.45);
}
:deep(.task-table .el-table__fixed-right-patch) {
	background-color: #e8f7f1 !important;
}

.resource-tags {
	display: flex;
	flex-wrap: wrap;
	gap: 6px;
}

.plan-window {
	display: flex;
	flex-direction: column;
	align-items: flex-start;
	gap: 2px;
	font-size: 13px;
	line-height: 1.4;
}
.plan-window__main {
	font-weight: 600;
	color: #145039;
}
.plan-window__sub {
	color: #647a70;
	font-size: 12px;
	background: #f0f7f4;
	padding: 2px 6px;
	border-radius: 4px;
}

.task-pagination {
	margin-top: 16px;
	display: flex;
	justify-content: flex-end;
}

@media (max-width: 992px) {
	.task-header {
		flex-direction: column;
		align-items: flex-start;
		gap: 12px;
	}
	.task-filter {
		flex-direction: column;
		align-items: stretch;
		.el-select,
		.el-input {
			width: 100%;
		}
	}
}
</style>
