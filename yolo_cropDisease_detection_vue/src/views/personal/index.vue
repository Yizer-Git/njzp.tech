<template>
	<div class="profile-page layout-padding">
		<div class="profile-card-wrapper">
			<el-card shadow="hover" header="个人信息" class="profile-card">
				<el-form ref="profileFormRef" :model="state.form" size="default" label-width="90px">
					<el-row :gutter="24">
						<el-col :xs="24" :md="24" class="mb20">
							<el-form-item label="头像">
								<div class="avatar-wrapper">
									<el-upload
										v-model="state.form.avatar"
										ref="uploadFile"
										class="avatar-uploader"
										action="http://localhost:9999/files/upload"
										:show-file-list="false"
										:on-success="handleAvatarSuccess"
									>
										<img v-if="imageUrl" :src="imageUrl" class="avatar" />
										<el-icon v-else><Plus /></el-icon>
									</el-upload>
								</div>
							</el-form-item>
						</el-col>
						<el-col :xs="24" :md="24" class="mb20">
							<el-form-item label="账号">
								<el-input v-model="state.form.username" placeholder="请输入账号" clearable />
							</el-form-item>
						</el-col>
						<el-col :xs="24" :md="24" class="mb20">
							<el-form-item label="密码">
								<el-input v-model="state.form.password" placeholder="请输入密码" clearable />
							</el-form-item>
						</el-col>
						<el-col :xs="24" :md="24" class="mb20">
							<el-form-item label="姓名">
								<el-input v-model="state.form.name" placeholder="请输入姓名" clearable />
							</el-form-item>
						</el-col>
						<el-col :xs="24" :md="24" class="mb20">
							<el-form-item label="性别">
								<el-input v-model="state.form.sex" placeholder="请输入性别" clearable />
							</el-form-item>
						</el-col>
						<el-col :xs="24" :md="24" class="mb20">
							<el-form-item label="邮箱">
								<el-input v-model="state.form.email" placeholder="请输入邮箱地址" clearable />
							</el-form-item>
						</el-col>
						<el-col :xs="24" :md="24" class="mb20">
							<el-form-item label="联系电话">
								<el-input v-model="state.form.tel" placeholder="请输入联系电话" clearable />
							</el-form-item>
						</el-col>
						<el-col :xs="24" :md="24" class="mb20">
							<el-form-item label="角色">
								<el-input v-model="state.form.role" disabled placeholder="当前角色" clearable />
							</el-form-item>
						</el-col>
					</el-row>
				</el-form>
				<el-button type="primary" @click="submitProfile" class="save-button">保存修改</el-button>
			</el-card>
		</div>
	</div>
</template>

<script setup lang="ts" name="personal">
import { reactive, ref, onMounted } from 'vue';
import type { UploadInstance, UploadProps } from 'element-plus';
import { ElMessage } from 'element-plus';
import request from '/@/utils/request';
import { useUserInfo } from '/@/stores/userInfo';
import { storeToRefs } from 'pinia';
import { Plus } from '@element-plus/icons-vue';

const imageUrl = ref('');
const uploadFile = ref<UploadInstance>();

const handleAvatarSuccess: UploadProps['onSuccess'] = (response, uploadFile) => {
	imageUrl.value = URL.createObjectURL(uploadFile.raw!);
	state.form.avatar = response.data;
};

const state = reactive({
	form: {} as Record<string, any>,
});
const stores = useUserInfo();
const { userInfos } = storeToRefs(stores);

const mapRoleToLabel = (role: string) => {
	if (role === 'admin') return '管理员';
	if (role === 'common') return '普通用户';
	if (role === 'others') return '其他用户';
	return role;
};

const mapLabelToRole = (label: string) => {
	if (label === '管理员') return 'admin';
	if (label === '普通用户') return 'common';
	if (label === '其他用户') return 'others';
	return label;
};

const loadProfile = () => {
	request.get('/api/user/' + userInfos.value.userName).then((res) => {
		if (res.code === 0) {
			state.form = res.data;
			state.form.role = mapRoleToLabel(state.form.role);
			imageUrl.value = state.form.avatar;
		} else {
			ElMessage.error(res.msg);
		}
	});
};

const submitProfile = () => {
	const payload = { ...state.form, role: mapLabelToRole(state.form.role) };
	request.post('/api/user/update', payload).then((res) => {
		if (res.code === 0) {
			ElMessage.success('修改成功');
			loadProfile();
		} else {
			ElMessage.error(res.msg);
		}
	});
};

onMounted(() => {
	loadProfile();
});
</script>

<style scoped lang="scss">
.profile-page {
	display: flex;
	justify-content: center;
	background: radial-gradient(circle, #e3f7ef 0%, #ffffff 100%);
}

.profile-card-wrapper {
	width: 60%;
}

.profile-card {
	background: radial-gradient(circle, #e3f7ef 0%, #ffffff 100%);
	border-radius: var(--next-radius-lg);
	display: flex;
	flex-direction: column;
	align-items: center;
}

.profile-card :deep(.el-card__header) {
	font-weight: 600;
	font-size: 16px;
}

.el-form {
	width: 75%;
	margin-left: 10%;
}

.avatar-wrapper {
	display: flex;
	align-items: center;
	justify-content: flex-start;
}

.avatar-uploader {
	font-size: 24px;
	color: #7d8c9b;
	width: 120px;
	height: 120px;
	display: flex;
	justify-content: center;
	align-items: center;
	border: 1px dashed #d9d9d9;
	border-radius: var(--next-radius-sm);
	cursor: pointer;
}

.avatar-uploader .el-upload:hover {
	border-color: #409eff;
	color: #409eff;
}

.avatar {
	width: 120px;
	height: 120px;
	border-radius: var(--next-radius-sm);
	object-fit: cover;
}

.save-button {
	align-self: flex-end;
	margin-right: 15%;
}

.mb20 {
	margin-bottom: 20px;
}
</style>
