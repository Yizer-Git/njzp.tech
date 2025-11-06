<template>
	<div class="disease-manage-page">
		<el-card shadow="hover">
			<template #header>
				<div class="card-header">
					<span>病害信息管理</span>
					<el-button type="primary" :icon="Plus" @click="handleAdd">新增病害</el-button>
				</div>
			</template>

			<!-- 搜索区域 -->
			<div class="search-bar">
				<el-input
					v-model="searchForm.diseaseName"
					placeholder="病害名称/代码"
					clearable
					style="width: 200px"
					@clear="handleSearch"
				/>
				<el-input
					v-model="searchForm.cropName"
					placeholder="作物名称"
					clearable
					style="width: 150px"
					@clear="handleSearch"
				/>
				<el-select
					v-model="searchForm.riskLevel"
					placeholder="风险等级"
					clearable
					style="width: 150px"
					@change="handleSearch"
				>
					<el-option label="高风险" value="HIGH" />
					<el-option label="中风险" value="MEDIUM" />
					<el-option label="低风险" value="LOW" />
				</el-select>
				<el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
				<el-button :icon="Refresh" @click="handleReset">重置</el-button>
			</div>

			<!-- 表格 -->
			<el-table :data="tableData" border stripe v-loading="loading" style="margin-top: 20px">
				<el-table-column type="index" label="序号" width="60" align="center" />
				<el-table-column prop="diseaseCode" label="病害代码" width="200" />
				<el-table-column prop="diseaseName" label="病害名称" width="180" />
				<el-table-column prop="cropName" label="作物" width="120" />
				<el-table-column prop="pathogenType" label="病原类型" width="120" />
				<el-table-column prop="riskLevel" label="风险等级" width="100" align="center">
					<template #default="{ row }">
						<el-tag v-if="row.riskLevel === 'HIGH'" type="danger">高风险</el-tag>
						<el-tag v-else-if="row.riskLevel === 'MEDIUM'" type="warning">中风险</el-tag>
						<el-tag v-else type="success">低风险</el-tag>
					</template>
				</el-table-column>
				<el-table-column prop="symptomSummary" label="症状概要" show-overflow-tooltip min-width="200" />
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
			width="800px"
			:close-on-click-modal="false"
			@close="handleDialogClose"
		>
			<el-form :model="form" :rules="rules" ref="formRef" label-width="140px">
				<el-row :gutter="20">
					<el-col :span="12">
						<el-form-item label="病害代码" prop="diseaseCode">
							<el-input v-model="form.diseaseCode" placeholder="如：CORN_BLIGHT" :disabled="isEdit" />
						</el-form-item>
					</el-col>
					<el-col :span="12">
						<el-form-item label="病害名称" prop="diseaseName">
							<el-input v-model="form.diseaseName" placeholder="如：Corn Blight" />
						</el-form-item>
					</el-col>
				</el-row>
				<el-row :gutter="20">
					<el-col :span="12">
						<el-form-item label="作物名称" prop="cropName">
							<el-select v-model="form.cropName" placeholder="请选择作物" style="width: 100%">
								<el-option label="玉米 (Corn)" value="Corn" />
								<el-option label="水稻 (Rice)" value="Rice" />
								<el-option label="小麦 (Wheat)" value="Wheat" />
								<el-option label="西红柿 (Tomato)" value="Tomato" />
								<el-option label="草莓 (Strawberry)" value="Strawberry" />
								<el-option label="柑橘 (Citrus)" value="Citrus" />
							</el-select>
						</el-form-item>
					</el-col>
					<el-col :span="12">
						<el-form-item label="病原类型" prop="pathogenType">
							<el-select v-model="form.pathogenType" placeholder="请选择" style="width: 100%">
								<el-option label="真菌 (Fungus)" value="Fungus" />
								<el-option label="细菌 (Bacteria)" value="Bacteria" />
								<el-option label="病毒 (Virus)" value="Virus" />
								<el-option label="卵菌 (Oomycete)" value="Oomycete" />
								<el-option label="害虫 (Insect)" value="Insect" />
								<el-option label="螨虫 (Mite)" value="Mite" />
							</el-select>
						</el-form-item>
					</el-col>
				</el-row>
				<el-form-item label="风险等级" prop="riskLevel">
					<el-radio-group v-model="form.riskLevel">
						<el-radio label="HIGH">高风险</el-radio>
						<el-radio label="MEDIUM">中风险</el-radio>
						<el-radio label="LOW">低风险</el-radio>
					</el-radio-group>
				</el-form-item>
				<el-form-item label="病害描述" prop="description">
					<el-input v-model="form.description" type="textarea" :rows="3" placeholder="病害的详细描述" />
				</el-form-item>
				<el-form-item label="症状概要" prop="symptomSummary">
					<el-input v-model="form.symptomSummary" type="textarea" :rows="3" placeholder="主要症状描述" />
				</el-form-item>
				<el-form-item label="气候风险因素">
					<el-input
						v-model="form.climateRiskFactors"
						type="textarea"
						:rows="2"
						placeholder="如：高温高湿，连续降雨"
					/>
				</el-form-item>
			</el-form>
			<template #footer>
				<el-button @click="dialogVisible = false">取消</el-button>
				<el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
			</template>
		</el-dialog>
	</div>
</template>

<script setup lang="ts" name="diseaseManage">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox, FormInstance } from 'element-plus';
import { Plus, Search, Refresh, Edit, Delete } from '@element-plus/icons-vue';
import type { DiseaseInfo } from '/@/api/disease';
import { getDiseaseList, createDisease, updateDisease, deleteDisease } from '/@/api/disease';

const loading = ref(false);
const submitLoading = ref(false);
const dialogVisible = ref(false);
const isEdit = ref(false);
const dialogTitle = ref('新增病害');
const formRef = ref<FormInstance>();

const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const tableData = ref<DiseaseInfo[]>([]);

const searchForm = reactive({
	diseaseName: '',
	cropName: '',
	riskLevel: '',
});

const form = reactive<DiseaseInfo>({
	diseaseCode: '',
	diseaseName: '',
	cropName: '',
	description: '',
	symptomSummary: '',
	pathogenType: '',
	riskLevel: '',
	climateRiskFactors: '',
});

const rules = {
	diseaseCode: [{ required: true, message: '请输入病害代码', trigger: 'blur' }],
	diseaseName: [{ required: true, message: '请输入病害名称', trigger: 'blur' }],
	cropName: [{ required: true, message: '请选择作物', trigger: 'change' }],
	pathogenType: [{ required: true, message: '请选择病原类型', trigger: 'change' }],
	riskLevel: [{ required: true, message: '请选择风险等级', trigger: 'change' }],
};

// 加载数据
const loadData = async () => {
	loading.value = true;
	try {
		const res: any = await getDiseaseList({
			pageNum: pageNum.value,
			pageSize: pageSize.value,
			...searchForm,
		});
		if (res.code === '0') {
			tableData.value = res.data.records;
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

// 搜索
const handleSearch = () => {
	pageNum.value = 1;
	loadData();
};

// 重置
const handleReset = () => {
	searchForm.diseaseName = '';
	searchForm.cropName = '';
	searchForm.riskLevel = '';
	handleSearch();
};

// 新增
const handleAdd = () => {
	isEdit.value = false;
	dialogTitle.value = '新增病害';
	resetForm();
	dialogVisible.value = true;
};

// 编辑
const handleEdit = (row: DiseaseInfo) => {
	isEdit.value = true;
	dialogTitle.value = '编辑病害';
	Object.assign(form, row);
	dialogVisible.value = true;
};

// 删除
const handleDelete = (row: DiseaseInfo) => {
	ElMessageBox.confirm(`确定删除病害 "${row.diseaseName}" 吗？`, '提示', {
		confirmButtonText: '确定',
		cancelButtonText: '取消',
		type: 'warning',
	})
		.then(async () => {
			try {
				const res: any = await deleteDisease(row.id!);
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
				? await updateDisease(form.id!, form)
				: await createDisease(form);

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
	form.diseaseCode = '';
	form.diseaseName = '';
	form.cropName = '';
	form.description = '';
	form.symptomSummary = '';
	form.pathogenType = '';
	form.riskLevel = '';
	form.climateRiskFactors = '';
};

// 对话框关闭
const handleDialogClose = () => {
	formRef.value?.resetFields();
	resetForm();
};

onMounted(() => {
	loadData();
});
</script>

<style scoped lang="scss">
.disease-manage-page {
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
