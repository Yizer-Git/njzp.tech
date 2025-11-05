<template>
	<div class="solution-manage-page">
		<el-card shadow="hover">
			<template #header>
				<div class="card-header">
					<span>防治方案管理</span>
					<el-button type="primary" :icon="Plus" @click="handleAdd">新增方案</el-button>
				</div>
			</template>

			<!-- 搜索区域 -->
			<div class="search-bar">
				<el-input
					v-model="searchForm.cropName"
					placeholder="作物名称"
					clearable
					style="width: 150px"
					@clear="handleSearch"
				/>
				<el-select
					v-model="searchForm.status"
					placeholder="方案状态"
					clearable
					style="width: 150px"
					@change="handleSearch"
				>
					<el-option label="启用" value="ACTIVE" />
					<el-option label="禁用" value="INACTIVE" />
					<el-option label="归档" value="ARCHIVED" />
				</el-select>
				<el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
				<el-button :icon="Refresh" @click="handleReset">重置</el-button>
			</div>

			<!-- 表格 -->
			<el-table :data="tableData" border stripe v-loading="loading" style="margin-top: 20px">
				<el-table-column type="index" label="序号" width="60" align="center" />
				<el-table-column prop="id" label="方案ID" width="80" align="center" />
				<el-table-column label="病害信息" width="200">
					<template #default="{ row }">
						<div>{{ row.diseaseName || `病害ID: ${row.diseaseId}` }}</div>
					</template>
				</el-table-column>
				<el-table-column prop="cropName" label="作物" width="120" />
				<el-table-column label="药剂信息" width="200" show-overflow-tooltip>
					<template #default="{ row }">
						<div>{{ row.remedyName || `药剂ID: ${row.remedyId}` }}</div>
					</template>
				</el-table-column>
				<el-table-column label="推荐剂量" width="140">
					<template #default="{ row }">
						{{ row.recommendedDosage }} {{ row.dosageUnit }}
					</template>
				</el-table-column>
				<el-table-column prop="applicationStage" label="施药时期" width="140" show-overflow-tooltip />
				<el-table-column label="预计成本" width="100" align="right">
					<template #default="{ row }">
						{{ row.expectedCost ? row.expectedCost.toFixed(2) : '-' }}
					</template>
				</el-table-column>
				<el-table-column label="状态" width="100" align="center">
					<template #default="{ row }">
						<el-switch
							v-model="row.status"
							active-value="ACTIVE"
							inactive-value="INACTIVE"
							@change="handleStatusChange(row)"
						/>
					</template>
				</el-table-column>
				<el-table-column label="操作" width="180" fixed="right" align="center">
					<template #default="{ row }">
						<el-button type="primary" link :icon="Edit" @click="handleEdit(row)">编辑</el-button>
						<el-button type="danger" link :icon="Delete" @click="handleDelete(row)">删除</el-button>
					</template>
				</el-table-column>
			</el-table>

			<!-- 分页 -->
			<div class="pagination">
				<el-pagination
					v-model:current-page="pageNum"
					v-model:page-size="pageSize"
					:page-sizes="[10, 20, 50, 100]"
					:total="total"
					layout="total, sizes, prev, pager, next, jumper"
					@size-change="handleSearch"
					@current-change="handleSearch"
				/>
			</div>
		</el-card>

		<!-- 新增/编辑对话框 -->
		<el-dialog
			v-model="dialogVisible"
			:title="dialogTitle"
			width="900px"
			:close-on-click-modal="false"
			@close="handleDialogClose"
		>
			<el-form :model="form" :rules="rules" ref="formRef" label-width="160px">
				<el-row :gutter="20">
					<el-col :span="12">
						<el-form-item label="病害" prop="diseaseId">
							<el-select
								v-model="form.diseaseId"
								placeholder="请选择病害"
								filterable
								style="width: 100%"
								@change="handleDiseaseChange"
							>
								<el-option
									v-for="disease in diseaseOptions"
									:key="disease.id"
									:label="disease.diseaseName"
									:value="disease.id"
								/>
							</el-select>
						</el-form-item>
					</el-col>
					<el-col :span="12">
						<el-form-item label="药剂" prop="remedyId">
							<el-select
								v-model="form.remedyId"
								placeholder="请选择药剂"
								filterable
								style="width: 100%"
							>
								<el-option
									v-for="remedy in remedyOptions"
									:key="remedy.id"
									:label="remedy.remedyName"
									:value="remedy.id"
								/>
							</el-select>
						</el-form-item>
					</el-col>
				</el-row>
				<el-row :gutter="20">
					<el-col :span="8">
						<el-form-item label="推荐剂量" prop="recommendedDosage">
							<el-input-number v-model="form.recommendedDosage" :min="0" :precision="2" style="width: 100%" />
						</el-form-item>
					</el-col>
					<el-col :span="8">
						<el-form-item label="剂量单位" prop="dosageUnit">
							<el-select v-model="form.dosageUnit" placeholder="单位" style="width: 100%">
								<el-option label="g/acre" value="g/acre" />
								<el-option label="ml/acre" value="ml/acre" />
								<el-option label="kg/ha" value="kg/ha" />
								<el-option label="L/ha" value="L/ha" />
							</el-select>
						</el-form-item>
					</el-col>
					<el-col :span="8">
						<el-form-item label="预计成本">
							<el-input-number v-model="form.expectedCost" :min="0" :precision="2" style="width: 100%" />
						</el-form-item>
					</el-col>
				</el-row>
				<el-row :gutter="20">
					<el-col :span="12">
						<el-form-item label="施药时期">
							<el-input v-model="form.applicationStage" placeholder="如：拔节期至抽穗期" />
						</el-form-item>
					</el-col>
					<el-col :span="12">
						<el-form-item label="施药时机">
							<el-input v-model="form.applicationTiming" placeholder="如：发病初期喷施" />
						</el-form-item>
					</el-col>
				</el-row>
				<el-form-item label="施药要点">
					<el-input v-model="form.notes" type="textarea" :rows="3" placeholder="施药注意事项和要点" />
				</el-form-item>
				<el-form-item label="天气限制">
					<el-input v-model="form.weatherConstraints" type="textarea" :rows="2" placeholder="如：避开降雨前2小时" />
				</el-form-item>
				<el-form-item label="方案状态" prop="status">
					<el-radio-group v-model="form.status">
						<el-radio label="ACTIVE">启用</el-radio>
						<el-radio label="INACTIVE">禁用</el-radio>
						<el-radio label="ARCHIVED">归档</el-radio>
					</el-radio-group>
				</el-form-item>
			</el-form>
			<template #footer>
				<el-button @click="dialogVisible = false">取消</el-button>
				<el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
			</template>
		</el-dialog>
	</div>
</template>

<script setup lang="ts" name="solutionManage">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox, FormInstance } from 'element-plus';
import { Plus, Search, Refresh, Edit, Delete } from '@element-plus/icons-vue';
import type { SolutionPlan } from '/@/api/solutionPlan';
import {
	getSolutionPlanList,
	createSolutionPlan,
	updateSolutionPlan,
	deleteSolutionPlan,
	updateSolutionPlanStatus,
} from '/@/api/solutionPlan';
import { getAllDiseases } from '/@/api/disease';
import { getAllRemedies } from '/@/api/remedy';

const loading = ref(false);
const submitLoading = ref(false);
const dialogVisible = ref(false);
const isEdit = ref(false);
const dialogTitle = ref('新增方案');
const formRef = ref<FormInstance>();

const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const tableData = ref<any[]>([]);
const diseaseOptions = ref<any[]>([]);
const remedyOptions = ref<any[]>([]);

const searchForm = reactive({
	cropName: '',
	status: '',
});

const form = reactive<SolutionPlan>({
	diseaseId: 0,
	remedyId: 0,
	cropName: '',
	recommendedDosage: 0,
	dosageUnit: 'g/acre',
	applicationStage: '',
	applicationTiming: '',
	notes: '',
	weatherConstraints: '',
	expectedCost: 0,
	status: 'ACTIVE',
});

const rules = {
	diseaseId: [{ required: true, message: '请选择病害', trigger: 'change' }],
	remedyId: [{ required: true, message: '请选择药剂', trigger: 'change' }],
	recommendedDosage: [{ required: true, message: '请输入推荐剂量', trigger: 'blur' }],
	dosageUnit: [{ required: true, message: '请选择剂量单位', trigger: 'change' }],
	status: [{ required: true, message: '请选择方案状态', trigger: 'change' }],
};

// 加载病害和药剂选项
const loadOptions = async () => {
	try {
		const [diseaseRes, remedyRes]: any = await Promise.all([getAllDiseases(), getAllRemedies()]);
		
		if (diseaseRes.code === '0') {
			diseaseOptions.value = diseaseRes.data;
		}
		if (remedyRes.code === '0') {
			remedyOptions.value = remedyRes.data;
		}
	} catch (error) {
		ElMessage.error('加载选项失败');
	}
};

// 加载数据
const loadData = async () => {
	loading.value = true;
	try {
		const res: any = await getSolutionPlanList({
			pageNum: pageNum.value,
			pageSize: pageSize.value,
			...searchForm,
		});
		if (res.code === '0') {
			// 关联病害和药剂名称
			tableData.value = res.data.records.map((item: any) => {
				const disease = diseaseOptions.value.find(d => d.id === item.diseaseId);
				const remedy = remedyOptions.value.find(r => r.id === item.remedyId);
				return {
					...item,
					diseaseName: disease?.diseaseName,
					remedyName: remedy?.remedyName,
				};
			});
			total.value = res.data.total;
		} else {
			ElMessage.error(res.msg || '加载失败');
		}
	} catch (error) {
		ElMessage.error('加载失败');
	} finally {
		loading.value = false;
	}
};

// 病害变更时自动填充作物
const handleDiseaseChange = (diseaseId: number) => {
	const disease = diseaseOptions.value.find(d => d.id === diseaseId);
	if (disease) {
		form.cropName = disease.cropName;
	}
};

// 状态变更
const handleStatusChange = async (row: any) => {
	try {
		const res: any = await updateSolutionPlanStatus(row.id, row.status);
		if (res.code === '0') {
			ElMessage.success('状态更新成功');
		} else {
			ElMessage.error(res.msg || '更新失败');
			// 恢复原状态
			row.status = row.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
		}
	} catch (error) {
		ElMessage.error('更新失败');
		row.status = row.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
	}
};

// 搜索
const handleSearch = () => {
	pageNum.value = 1;
	loadData();
};

// 重置
const handleReset = () => {
	searchForm.cropName = '';
	searchForm.status = '';
	handleSearch();
};

// 新增
const handleAdd = () => {
	isEdit.value = false;
	dialogTitle.value = '新增方案';
	resetForm();
	dialogVisible.value = true;
};

// 编辑
const handleEdit = (row: any) => {
	isEdit.value = true;
	dialogTitle.value = '编辑方案';
	Object.assign(form, row);
	dialogVisible.value = true;
};

// 删除
const handleDelete = (row: any) => {
	ElMessageBox.confirm(`确定删除该防治方案吗？`, '提示', {
		confirmButtonText: '确定',
		cancelButtonText: '取消',
		type: 'warning',
	})
		.then(async () => {
			try {
				const res: any = await deleteSolutionPlan(row.id);
				if (res.code === '0') {
					ElMessage.success('删除成功');
					loadData();
				} else {
					ElMessage.error(res.msg || '删除失败');
				}
			} catch (error) {
				ElMessage.error('删除失败');
			}
		})
		.catch(() => {});
};

// 提交
const handleSubmit = async () => {
	if (!formRef.value) return;
	
	await formRef.value.validate(async (valid) => {
		if (!valid) return;

		submitLoading.value = true;
		try {
			const res: any = isEdit.value
				? await updateSolutionPlan(form.id!, form)
				: await createSolutionPlan(form);

			if (res.code === '0') {
				ElMessage.success(isEdit.value ? '更新成功' : '新增成功');
				dialogVisible.value = false;
				loadData();
			} else {
				ElMessage.error(res.msg || '操作失败');
			}
		} catch (error) {
			ElMessage.error('操作失败');
		} finally {
			submitLoading.value = false;
		}
	});
};

// 重置表单
const resetForm = () => {
	form.id = undefined;
	form.diseaseId = 0;
	form.remedyId = 0;
	form.cropName = '';
	form.recommendedDosage = 0;
	form.dosageUnit = 'g/acre';
	form.applicationStage = '';
	form.applicationTiming = '';
	form.notes = '';
	form.weatherConstraints = '';
	form.expectedCost = 0;
	form.status = 'ACTIVE';
};

// 对话框关闭
const handleDialogClose = () => {
	formRef.value?.resetFields();
	resetForm();
};

onMounted(async () => {
	await loadOptions();
	loadData();
});
</script>

<style scoped lang="scss">
.solution-manage-page {
	padding: 20px;

	.card-header {
		display: flex;
		justify-content: space-between;
		align-items: center;
	}

	.search-bar {
		display: flex;
		gap: 12px;
		flex-wrap: wrap;
	}

	.pagination {
		margin-top: 20px;
		display: flex;
		justify-content: flex-end;
	}
}
</style>

