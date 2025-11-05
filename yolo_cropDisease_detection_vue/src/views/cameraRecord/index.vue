<template>
	<div class="page-shell">
		<el-card shadow="never" class="page-card">
			<header class="page-card__header">
				<div class="page-card__heading">
					<h2>摄像识别记录</h2>
					<p>追踪实时摄像识别的最新结果，掌握生产现场的动态变化。</p>
				</div>
				<el-button type="primary" @click="getTableData">
					<el-icon><ele-Refresh /></el-icon>
					刷新数据
				</el-button>
			</header>
			<header class="page-card__toolbar">
				<div class="page-filters">
					<el-input v-model="state.tableData.param.search1" size="large" placeholder="按作物类型筛选">
						<template #prefix>
							<el-icon><ele-Collection /></el-icon>
						</template>
					</el-input>
					<el-input v-model="state.tableData.param.search3" size="large" placeholder="按最低置信度过滤">
						<template #prefix>
							<el-icon><ele-DataLine /></el-icon>
						</template>
					</el-input>
					<el-button size="large" type="primary" @click="getTableData">
						<el-icon><ele-Search /></el-icon>
						查询记录
					</el-button>
				</div>
				<div class="page-card__extra">
					<el-tooltip content="导出 CSV">
						<el-button circle plain @click="onExport">
							<el-icon><ele-Download /></el-icon>
						</el-button>
					</el-tooltip>
				</div>
			</header>
			<el-table
				:data="state.tableData.data"
				v-loading="state.tableData.loading"
				class="page-table"
				style="width: 100%"
			>
				<el-table-column prop="num" label="#" width="70" align="center" />
				<el-table-column prop="outVideo" label="识别结果" width="220" align="center">
					<template #default="scope">
						<div class="media-box">
							<video class="media-box__player" preload="auto" controls :key="scope.row.outVideo + uniqueKey">
								<source :src="scope.row.outVideo" type="video/mp4" />
							</video>
						</div>
					</template>
				</el-table-column>
				<el-table-column prop="kind" label="作物类型" align="center" />
				<el-table-column prop="weight" label="识别权重" align="center" />
				<el-table-column prop="conf" label="最小置信" show-overflow-tooltip width="110" align="center" />
				<el-table-column prop="username" label="识别用户" show-overflow-tooltip align="center" />
				<el-table-column prop="startTime" label="识别时间" show-overflow-tooltip align="center" />
				<el-table-column label="操作" width="140" align="center">
					<template #default="scope">
						<el-button size="small" text type="danger" @click="onRowDel(scope.row)">
							<el-icon><ele-Delete /></el-icon>
							删除
						</el-button>
					</template>
				</el-table-column>
			</el-table>
			<div class="page-pagination">
				<el-pagination
					@size-change="onHandleSizeChange"
					@current-change="onHandleCurrentChange"
					:pager-count="5"
					:page-sizes="[10, 20, 30]"
					v-model:current-page="state.tableData.param.pageNum"
					background
					v-model:page-size="state.tableData.param.pageSize"
					layout="total, sizes, prev, pager, next, jumper"
					:total="state.tableData.total"
				>
				</el-pagination>
			</div>
		</el-card>
	</div>
</template>

<script setup lang="ts" name="systemRole">
import { reactive, onMounted, ref } from 'vue';
import { ElMessageBox, ElMessage } from 'element-plus';
import request from '/@/utils/request';
import { useUserInfo } from '/@/stores/userInfo';
import { storeToRefs } from 'pinia';

const stores = useUserInfo();
const { userInfos } = storeToRefs(stores);

const state = reactive<SysRoleState>({
	tableData: {
		data: [] as any,
		total: 0,
		loading: false,
		param: {
			search: '',
			search3: '',
			search2: '',
			pageNum: 1,
			pageSize: 10,
		},
	},
});

const uniqueKey = ref(0);

const getTableData = () => {
	state.tableData.loading = true;
	if (userInfos.value.userName !== 'admin') {
		state.tableData.param.search = userInfos.value.userName;
	}
	request
		.get('/api/cameraRecords', {
			params: state.tableData.param,
		})
		.then((res) => {
			console.log('摄像记录API响应:', res);
			if (res.code === 0) {
				state.tableData.data = [];
				setTimeout(() => {
					state.tableData.loading = false;
				}, 500);
				
				// 检查数据是否存在
				if (res.data && res.data.records && res.data.records.length > 0) {
					for (let i = 0; i < res.data.records.length; i++) {
						state.tableData.data[i] = res.data.records[i];
						state.tableData.data[i].num = i + 1;
					}
					state.tableData.total = res.data.total;
				} else {
					console.log('没有找到摄像记录数据');
					state.tableData.total = 0;
				}
				uniqueKey.value++;
			} else {
				console.error('API返回错误:', res.msg);
				ElMessage({
					type: 'error',
					message: res.msg || '获取摄像记录失败',
				});
				state.tableData.loading = false;
			}
		})
		.catch((error) => {
			console.error('请求摄像记录失败:', error);
			ElMessage({
				type: 'error',
				message: '网络请求失败，请检查后端服务是否正常运行',
			});
			state.tableData.loading = false;
		});
};

const onExport = () => {
	ElMessage.info('导出功能开发中');
};

const onRowDel = (row: any) => {
	ElMessageBox.confirm(`此操作将永久删除该信息, 是否继续?`, '提示', {
		confirmButtonText: '确认',
		cancelButtonText: '取消',
		type: 'warning',
	})
		.then(() => {
			request.delete('/api/cameraRecords/' + row.id).then((res) => {
				if (res.code === 0) {
					ElMessage({
						type: 'success',
						message: '删除成功',
					});
				} else {
					ElMessage({
						type: 'error',
						message: res.msg,
					});
				}
			});
			setTimeout(() => {
				getTableData();
			}, 500);
		})
		.catch(() => {});
};

const onHandleSizeChange = (val: number) => {
	state.tableData.param.pageSize = val;
	getTableData();
};

const onHandleCurrentChange = (val: number) => {
	state.tableData.param.pageNum = val;
	getTableData();
};

onMounted(() => {
	getTableData();
});
</script>

<style scoped lang="scss">
.page-shell {
	display: flex;
	flex-direction: column;
	gap: 24px;
	width: 100%;
	height: 100%;
}

.page-card {
	flex: 1;
	display: flex;
	flex-direction: column;
	border-radius: var(--next-radius-md);
	padding: 0;
	border: 1px solid rgba(32, 201, 151, 0.08);
}

.page-card__header {
	display: flex;
	align-items: flex-start;
	justify-content: space-between;
	gap: 16px;
	padding: 24px 24px 0;

	:deep(.el-button) {
		flex-shrink: 0;
	}
}

.page-card__heading {
	display: flex;
	flex-direction: column;
	gap: 6px;
	max-width: 60%;
	h2 {
		margin: 0;
		font-size: 26px;
		font-weight: 700;
		color: #22324a;
	}
	p {
		margin: 0;
		color: #6b7a94;
		font-size: 14px;
		line-height: 1.6;
	}
}

.page-card__toolbar {
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding: 16px 24px 0;
	margin-top: 12px;
	gap: 16px;
}

.page-filters {
	display: flex;
	align-items: center;
	gap: 12px;
	flex: 1;

	:deep(.el-input) {
		max-width: 240px;
	}
}

.page-card__extra {
	display: flex;
	gap: 8px;

	:deep(.el-button) {
		border-radius: 50%;
		height: 44px;
		width: 44px;
		display: flex;
		align-items: center;
		justify-content: center;
		color: #1aa67f;
		border: 1px solid rgba(32, 201, 151, 0.2);
	}
}

.page-table {
	padding: 16px 24px 0;
	flex: 1;
}

.media-box {
	width: 180px;
	border-radius: var(--next-radius-lg);
	overflow: hidden;
	background: #f5f9f8;
	border: 1px solid rgba(32, 201, 151, 0.1);
	box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.6);

	&__player {
		width: 100%;
		height: 110px;
		object-fit: cover;
	}
}

.page-pagination {
	padding: 16px 24px 24px;
	display: flex;
	justify-content: flex-end;

	:deep(.el-pagination.is-background) {
		--record-green: #1aa67f;
		display: flex;
		align-items: center;
		gap: 8px;

		.btn-prev,
		.btn-next,
		.el-pager li {
			min-width: 36px;
			height: 36px;
			border-radius: var(--next-radius-md);
			border: 1px solid transparent;
			transition: all 0.2s ease;
		}

		.btn-prev,
		.btn-next {
			color: #4d6a7c;
		}

		.el-pager li {
			background-color: #eef9f5;
			color: #2a4c5a;
		}

		.el-pager li:not(.is-active):hover {
			background-color: #daf2ea;
			color: var(--record-green);
		}

		.el-pager li.is-active {
			background-color: var(--record-green);
			color: #ffffff;
			box-shadow: 0 6px 14px rgba(26, 166, 127, 0.18);
		}

		.el-pagination__total,
		.el-pagination__sizes,
		.el-pagination__jump {
			color: #4d6a7c;
		}

		.el-select .el-input {
			width: 110px;

			.el-input__wrapper {
				border-radius: var(--next-radius-sm);
				box-shadow: inset 0 0 0 1px rgba(26, 166, 127, 0.12);
			}

			.el-input__wrapper:hover {
				box-shadow: inset 0 0 0 1px rgba(26, 166, 127, 0.28);
			}
		}

		.el-pagination__jump .el-input .el-input__wrapper {
			border-radius: var(--next-radius-sm);
			box-shadow: inset 0 0 0 1px rgba(26, 166, 127, 0.12);
		}

		.el-pagination__jump .el-input .el-input__wrapper:hover {
			box-shadow: inset 0 0 0 1px rgba(26, 166, 127, 0.28);
		}
	}
}
</style>
