<template>
	<div class="layout-navbars-breadcrumb-user pr15" :style="{ flex: layoutUserFlexNum }">
		<el-dropdown :show-timeout="70" :hide-timeout="50" trigger="click" @command="onComponentSizeChange" class="custom-dropdown">
			<div class="layout-navbars-breadcrumb-user-icon">
				<i class="iconfont icon-Text" :title="$t('message.user.title0')"></i>
			</div>
			<template #dropdown>
				<el-dropdown-menu>
					<el-dropdown-item command="large" :disabled="state.disabledSize === 'large'">{{ $t('message.user.dropdownLarge') }}</el-dropdown-item>
					<el-dropdown-item command="default" :disabled="state.disabledSize === 'default'">{{ $t('message.user.dropdownDefault') }}</el-dropdown-item>
					<el-dropdown-item command="small" :disabled="state.disabledSize === 'small'">{{ $t('message.user.dropdownSmall') }}</el-dropdown-item>
				</el-dropdown-menu>
			</template>
		</el-dropdown>
		<div class="layout-navbars-breadcrumb-user-icon" @click="onSearchClick">
			<el-icon :title="$t('message.user.title2')">
				<ele-Search />
			</el-icon>
		</div>
		<div class="layout-navbars-breadcrumb-user-icon" @click="onLayoutSetingClick">
			<i class="icon-skin iconfont" :title="$t('message.user.title3')"></i>
		</div>
		<!-- <div class="layout-navbars-breadcrumb-user-icon">
			<el-popover placement="bottom" trigger="click" transition="el-zoom-in-top" :width="300" :persistent="false">
				<template #reference>
					<el-badge :is-dot="true">
						<el-icon :title="$t('message.user.title4')">
							<ele-Bell />
						</el-icon>
					</el-badge>
				</template>
				<template #default>
					<UserNews />
				</template>
			</el-popover>
		</div> -->
		<div class="layout-navbars-breadcrumb-user-icon mr10" @click="onScreenfullClick">
			<i
				class="iconfont"
				:title="state.isScreenfull ? $t('message.user.title6') : $t('message.user.title5')"
				:class="!state.isScreenfull ? 'icon-APP' : 'icon-Menu'"
			></i>
		</div>
		<el-dropdown :show-timeout="70" :hide-timeout="50" @command="onHandleCommandClick" class="custom-dropdown">
			<span class="layout-navbars-breadcrumb-user-link">
				<img :src="state.img" class="layout-navbars-breadcrumb-user-link-photo mr5" />
				{{ username }}
				<el-icon class="el-icon--right">
					<ele-ArrowDown />
				</el-icon>
			</span>
			<template #dropdown>
				<el-dropdown-menu>
					<el-dropdown-item command="/">{{ $t('message.user.dropdown1') }}</el-dropdown-item>
					<el-dropdown-item divided command="logOut">{{ $t('message.user.dropdown5') }}</el-dropdown-item>
				</el-dropdown-menu>
			</template>
		</el-dropdown>
		<Search ref="searchRef" />
	</div>
</template>

<script setup lang="ts" name="layoutBreadcrumbUser">
import { defineAsyncComponent, ref, computed, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessageBox, ElMessage } from 'element-plus';
import { useI18n } from 'vue-i18n';
import { storeToRefs } from 'pinia';
import { useUserInfo } from '/@/stores/userInfo';
import { useThemeConfig } from '/@/stores/themeConfig';
import other from '/@/utils/other';
import request from '/@/utils/request';
import { Session, Local } from '/@/utils/storage';
import Cookies from 'js-cookie';
import screenfull from 'screenfull';
import mittBus from '/@/utils/mitt';
// 引入组件
// const UserNews = defineAsyncComponent(() => import('/@/layout/navBars/breadcrumb/userNews.vue'));
const Search = defineAsyncComponent(() => import('/@/layout/navBars/breadcrumb/search.vue'));

// 定义变量内容
const { locale, t } = useI18n();
const router = useRouter();
const stores = useUserInfo();
const storesThemeConfig = useThemeConfig();
const { userInfos } = storeToRefs(stores);
const { themeConfig } = storeToRefs(storesThemeConfig);
const searchRef = ref();
const state = reactive({
	img: '',
	isScreenfull: false,
	disabledI18n: 'zh-cn',
	disabledSize: 'large',
});

let username: string = '';

// 设置分割样式
const layoutUserFlexNum = computed(() => {
	let num: string | number = '';
	const { layout, isClassicSplitMenu } = themeConfig.value;
	const layoutArr: string[] = ['defaults', 'columns'];
	if (layoutArr.includes(layout) || (layout === 'classic' && !isClassicSplitMenu)) num = '1';
	else num = '';
	return num;
});
// 布局配置 icon 点击时
const onLayoutSetingClick = () => {
	mittBus.emit('openSetingsDrawer');
};
// 下拉菜单点击时
const onHandleCommandClick = (path: string) => {
	if (path === 'logOut') {
		ElMessageBox({
			closeOnClickModal: false,
			closeOnPressEscape: false,
			title: t('message.user.logOutTitle'),
			message: t('message.user.logOutMessage'),
			showCancelButton: true,
			confirmButtonText: t('message.user.logOutConfirm'),
			cancelButtonText: t('message.user.logOutCancel'),
			buttonSize: 'default',
			beforeClose: (action, instance, done) => {
				if (action === 'confirm') {
					instance.confirmButtonLoading = true;
					instance.confirmButtonText = t('message.user.logOutExit');
					setTimeout(() => {
						done();
						setTimeout(() => {
							instance.confirmButtonLoading = false;
						}, 300);
					}, 700);
				} else {
					done();
				}
			},
		})
			.then(async () => {
				// 清除缓存/token等
				Session.clear();
				// 使用 reload 时，不需要调用 resetRoute() 重置路由
				window.location.reload();
			})
			.catch(() => {});
	} else {
		router.push(path);
	}
};
// 菜单搜索点击
const onSearchClick = () => {
	searchRef.value.openSearch();
};
// 组件大小改变
const onComponentSizeChange = (size: string) => {
	Local.remove('themeConfig');
	themeConfig.value.globalComponentSize = size;
	Local.set('themeConfig', themeConfig.value);
	initI18nOrSize('globalComponentSize', 'disabledSize');
	window.location.reload();
};
// 全屏点击时
const onScreenfullClick = () => {
	if (!screenfull.isEnabled) {
		ElMessage.warning('暂不不支持全屏');
		return false;
	}
	screenfull.toggle();
	screenfull.on('change', () => {
		if (screenfull.isFullscreen) state.isScreenfull = true;
		else state.isScreenfull = false;
	});
};
// 语言切换
const onLanguageChange = (lang: string) => {
	Local.remove('themeConfig');
	themeConfig.value.globalI18n = lang;
	Local.set('themeConfig', themeConfig.value);
	locale.value = lang;
	other.useTitle();
	initI18nOrSize('globalI18n', 'disabledI18n');
};
// 初始化组件大小/i18n
const initI18nOrSize = (value: string, attr: string) => {
	state[attr] = Local.get('themeConfig')[value];
};
const getTableData = () => {
	request.get('/api/user/' + userInfos.value.userName).then((res) => {
		// console.log(res);
		if (res.code == 0) {
			state.img = res.data.avatar;
		} else {
			ElMessage({
				type: 'error',
				message: res.msg,
			});
		}
	});
};
// 页面加载时
onMounted(() => {
	// console.log(userInfos.value);
	username = userInfos.value.userName
	getTableData();
	if (Local.get('themeConfig')) {
		initI18nOrSize('globalComponentSize', 'disabledSize');
		initI18nOrSize('globalI18n', 'disabledI18n');
	}
});
</script>

<style scoped lang="scss">
.tile {
	width: 100%;
	height: 100%;
	color: black;
	font-size: 20px;
	font-weight: 600;
	margin-left: -150px;
	display: flex;
	justify-content: flex-start;
	align-items: center;
	text-align: center;
}
.layout-navbars-breadcrumb-user {
	display: flex;
	align-items: center;
	justify-content: flex-end;
	gap: 4px;
	&-link {
		height: 100%;
		display: flex;
		align-items: center;
		white-space: nowrap;
		font-weight: 500;
		color: #445263;
		transition: color 0.2s ease;
		&:hover {
			color: #1aa67f;
		}
		&-photo {
			width: 42px;
			height: 42px;
			border-radius: 50%;
			margin-right: 12px;
			box-shadow: 0 8px 18px -12px rgba(32, 201, 151, 0.5);
		}
	}
	&-icon {
		padding: 0 12px;
		cursor: pointer;
		color: #5f6b7c !important;
		height: 72px;
		line-height: 72px;
		display: flex;
		align-items: center;
		border-radius: var(--next-radius-lg);
		transition: background-color 0.2s ease, color 0.2s ease;
		&:hover {
			background: rgba(32, 201, 151, 0.12) !important;
			color: #1aa67f !important;
		}
		i,
		.el-icon {
			font-size: 18px;
		}
	}
	:deep(.el-dropdown) {
		color: inherit;
	}
	:deep(.el-badge) {
		height: 42px;
		line-height: 42px;
		display: flex;
		align-items: center;
	}
	:deep(.el-badge__content.is-fixed) {
		top: 16px;
	}
}
.custom-dropdown {
	:deep(.el-dropdown-link) {
		color: inherit;
		display: flex;
		align-items: center;
		gap: 6px;
		cursor: pointer;
	}

	:deep(.el-dropdown-menu) {
		background-color: #ffffff;
		border: 1px solid rgba(32, 201, 151, 0.12);
		border-radius: var(--next-radius-lg);
		box-shadow: 0 18px 36px -24px rgba(23, 89, 70, 0.2);
		overflow: hidden;
		padding: 4px 0;
	}

	:deep(.el-dropdown-menu__item) {
		color: #4a5a70 !important;
		padding: 10px 18px;
		transition: background-color 0.2s ease, color 0.2s ease;
		display: flex;
		align-items: center;
		gap: 6px;
		&:hover {
			background-color: rgba(32, 201, 151, 0.08) !important;
			color: #1aa67f !important;
		}
	}
}
</style>
