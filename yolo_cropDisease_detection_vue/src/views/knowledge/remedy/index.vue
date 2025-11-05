<template>
	<div class="remedy-manage-page">
		<el-card shadow="hover">
			<template #header>
				<div class="card-header">
					<span>药剂信息管理</span>
					<el-button type="primary" :icon="Plus" @click="handleAdd">新增药剂</el-button>
				</div>
			</template>

			<!-- 搜索区域 -->
			<div class="search-bar">
				<el-input
					v-model="searchForm.remedyName"
					placeholder="药剂名称/代码"
					clearable
					style="width: 200px"
					@clear="handleSearch"
				/>
				<el-input
					v-model="searchForm.activeIngredient"
					placeholder="有效成分"
					clearable
					style="width: 150px"
					@clear="handleSearch"
				/>
				<el-input
					v-model="searchForm.targetPathogen"
					placeholder="目标病原"
					clearable
					style="width: 150px"
					@clear="handleSearch"
				/>
				<el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
				<el-button :icon="Refresh" @click="handleReset">重置</el-button>
			</div>

			<!-- 表格 -->
			<el-table :data="tableData" border stripe v-loading="loading" style="margin-top: 20px">
				<el-table-column type="index" label="序号" width="60" align="center" />
				<el-table-column prop="remedyCode" label="药剂代码" width="180" show-overflow-tooltip />
				<el-table-column prop="remedyName" label="药剂名称" width="200" show-overflow-tooltip />
				<el-table-column prop="activeIngredient" label="有效成分" width="150" show-overflow-tooltip />
				<el-table-column prop="targetPathogen" label="目标病原" width="150" show-overflow-tooltip />
				<el-table-column prop="formulation" label="剂型" width="120" />
				<el-table-column label="安全剂量" width="120">
					<template #default="{ row }">
						{{ row.safeDosage }} {{ row.dosageUnit }}
					</template>
				</el-table-column>
				<el-table-column label="价格" width="120" align="right">
					<template #default="{ row }">
						<el-popover placement="top" :width="200" trigger="hover">
							<template #reference>
								<span class="price-text">{{ row.costPerUnit }} {{ row.currency }}</span>
							</template>
							<div>
								<p>最后更新: {{ formatDate(row.lastPriceUpdate) }}</p>
								<el-button
									type="primary"
									size="small"
									link
									@click="handleUpdatePrice(row)"
								>
									更新价格
								</el-button>
							</div>
						</el-popover>
					</template>
				</el-table-column>
				<el-table-column prop="intervalDays" label="间隔期(天)" width="100" align="center" />
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
						<el-form-item label="药剂代码" prop="remedyCode">
							<el-input v-model="form.remedyCode" placeholder="如：AZOXYSTROBIN_200SC" :disabled="isEdit" />
						</el-form-item>
					</el-col>
					<el-col :span="12">
						<el-form-item label="药剂名称" prop="remedyName">
							<el-input v-model="form.remedyName" placeholder="如：嘧菌酯200g/L SC" />
						</el-form-item>
					</el-col>
				</el-row>
				<el-row :gutter="20">
					<el-col :span="12">
						<el-form-item label="有效成分" prop="activeIngredient">
							<el-input v-model="form.activeIngredient" placeholder="如：Azoxystrobin" />
						</el-form-item>
					</el-col>
					<el-col :span="12">
						<el-form-item label="目标病原">
							<el-input v-model="form.targetPathogen" placeholder="如：Puccinia sorghi" />
						</el-form-item>
					</el-col>
				</el-row>
				<el-row :gutter="20">
					<el-col :span="12">
						<el-form-item label="剂型">
							<el-select v-model="form.formulation" placeholder="请选择" style="width: 100%">
								<el-option label="可湿性粉剂 (WP)" value="Wettable powder" />
								<el-option label="悬浮剂 (SC)" value="Suspension concentrate" />
								<el-option label="乳油 (EC)" value="Emulsifiable concentrate" />
								<el-option label="水分散粒剂 (WG)" value="Water dispersible granule" />
								<el-option label="水剂 (AS)" value="Aqueous solution" />
							</el-select>
						</el-form-item>
					</el-col>
					<el-col :span="12">
						<el-form-item label="施用方法">
							<el-input v-model="form.applicationMethod" placeholder="如：叶面喷雾" />
						</el-form-item>
					</el-col>
				</el-row>
				<el-row :gutter="20">
					<el-col :span="8">
						<el-form-item label="安全剂量" prop="safeDosage">
							<el-input-number v-model="form.safeDosage" :min="0" :precision="2" style="width: 100%" />
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
						<el-form-item label="施药间隔(天)" prop="intervalDays">
							<el-input-number v-model="form.intervalDays" :min="1" style="width: 100%" />
						</el-form-item>
					</el-col>
				</el-row>
				<el-row :gutter="20">
					<el-col :span="8">
						<el-form-item label="安全间隔期(天)">
							<el-input-number v-model="form.safetyIntervalDays" :min="0" style="width: 100%" />
						</el-form-item>
					</el-col>
					<el-col :span="8">
						<el-form-item label="单季最多次数">
							<el-input-number v-model="form.maxApplicationsPerSeason" :min="1" style="width: 100%" />
						</el-form-item>
					</el-col>
					<el-col :span="8">
						<el-form-item label="单价" prop="costPerUnit">
							<el-input-number v-model="form.costPerUnit" :min="0" :precision="2" style="width: 100%" />
						</el-form-item>
					</el-col>
				</el-row>
				<el-form-item label="注意事项">
					<el-input v-model="form.caution" type="textarea" :rows="3" placeholder="施药注意事项" />
				</el-form-item>
			</el-form>
			<template #footer>
				<el-button @click="dialogVisible = false">取消</el-button>
				<el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
			</template>
		</el-dialog>

		<!-- 价格更新对话框 -->
		<el-dialog v-model="priceDialogVisible" title="更新价格" width="400px">
			<el-form label-width="100px">
				<el-form-item label="当前价格">
					<span>{{ currentRemedy?.costPerUnit }} {{ currentRemedy?.currency }}</span>
				</el-form-item>
				<el-form-item label="新价格">
					<el-input-number v-model="newPrice" :min="0" :precision="2" style="width: 100%" />
				</el-form-item>
			</el-form>
			<template #footer>
				<el-button @click="priceDialogVisible = false">取消</el-button>
				<el-button type="primary" @click="handlePriceSubmit" :loading="priceLoading">确定</el-button>
			</template>
		</el-dialog>
	</div>
</template>

<script setup lang="ts" name="remedyManage">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox, FormInstance } from 'element-plus';
import { Plus, Search, Refresh, Edit, Delete } from '@element-plus/icons-vue';
import type { Remedy } from '/@/api/remedy';
import { getRemedyList, createRemedy, updateRemedy, deleteRemedy, updateRemedyPrice } from '/@/api/remedy';

const loading = ref(false);
const submitLoading = ref(false);
const priceLoading = ref(false);
const dialogVisible = ref(false);
const priceDialogVisible = ref(false);
const isEdit = ref(false);
const dialogTitle = ref('新增药剂');
const formRef = ref<FormInstance>();

const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const tableData = ref<Remedy[]>([]);
const currentRemedy = ref<Remedy | null>(null);
const newPrice = ref(0);

const searchForm = reactive({
	remedyName: '',
	activeIngredient: '',
	targetPathogen: '',
});

const form = reactive<Remedy>({
	remedyCode: '',
	remedyName: '',
	activeIngredient: '',
	targetPathogen: '',
	formulation: '',
	safeDosage: 0,
	dosageUnit: 'g/acre',
	intervalDays: 7,
	applicationMethod: '',
	safetyIntervalDays: 7,
	maxApplicationsPerSeason: 3,
	caution: '',
	costPerUnit: 0,
	currency: 'CNY',
});

const rules = {
	remedyCode: [{ required: true, message: '请输入药剂代码', trigger: 'blur' }],
	remedyName: [{ required: true, message: '请输入药剂名称', trigger: 'blur' }],
	activeIngredient: [{ required: true, message: '请输入有效成分', trigger: 'blur' }],
	safeDosage: [{ required: true, message: '请输入安全剂量', trigger: 'blur' }],
	dosageUnit: [{ required: true, message: '请选择剂量单位', trigger: 'change' }],
	intervalDays: [{ required: true, message: '请输入施药间隔', trigger: 'blur' }],
	costPerUnit: [{ required: true, message: '请输入单价', trigger: 'blur' }],
};

// 格式化日期
const formatDate = (dateStr: string) => {
	if (!dateStr) return '-';
	return new Date(dateStr).toLocaleDateString('zh-CN');
};

// 加载数据
const loadData = async () => {
	loading.value = true;
	try {
		const res: any = await getRemedyList({
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
	searchForm.remedyName = '';
	searchForm.activeIngredient = '';
	searchForm.targetPathogen = '';
	handleSearch();
};

// 新增
const handleAdd = () => {
	isEdit.value = false;
	dialogTitle.value = '新增药剂';
	resetForm();
	dialogVisible.value = true;
};

// 编辑
const handleEdit = (row: Remedy) => {
	isEdit.value = true;
	dialogTitle.value = '编辑药剂';
	Object.assign(form, row);
	dialogVisible.value = true;
};

// 删除
const handleDelete = (row: Remedy) => {
	ElMessageBox.confirm(`确定删除药剂 "${row.remedyName}" 吗？`, '提示', {
		confirmButtonText: '确定',
		cancelButtonText: '取消',
		type: 'warning',
	})
		.then(async () => {
			try {
				const res: any = await deleteRemedy(row.id!);
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

// 更新价格
const handleUpdatePrice = (row: Remedy) => {
	currentRemedy.value = row;
	newPrice.value = row.costPerUnit;
	priceDialogVisible.value = true;
};

// 提交价格更新
const handlePriceSubmit = async () => {
	if (!currentRemedy.value) return;
	
	priceLoading.value = true;
	try {
		const res: any = await updateRemedyPrice(currentRemedy.value.id!, newPrice.value);
		if (res.code === '0') {
			ElMessage.success('价格更新成功');
			priceDialogVisible.value = false;
			loadData();
		} else {
			ElMessage.error(res.msg || '更新失败');
		}
	} catch (error) {
		ElMessage.error('更新失败');
	} finally {
		priceLoading.value = false;
	}
};

// 提交
const handleSubmit = async () => {
	if (!formRef.value) return;
	
	await formRef.value.validate(async (valid) => {
		if (!valid) return;

		submitLoading.value = true;
		try {
			const res: any = isEdit.value
				? await updateRemedy(form.id!, form)
				: await createRemedy(form);

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
	form.remedyCode = '';
	form.remedyName = '';
	form.activeIngredient = '';
	form.targetPathogen = '';
	form.formulation = '';
	form.safeDosage = 0;
	form.dosageUnit = 'g/acre';
	form.intervalDays = 7;
	form.applicationMethod = '';
	form.safetyIntervalDays = 7;
	form.maxApplicationsPerSeason = 3;
	form.caution = '';
	form.costPerUnit = 0;
	form.currency = 'CNY';
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
.remedy-manage-page {
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

	.price-text {
		color: #f56c6c;
		font-weight: 600;
		cursor: pointer;
		
		&:hover {
			text-decoration: underline;
		}
	}
}
</style>

