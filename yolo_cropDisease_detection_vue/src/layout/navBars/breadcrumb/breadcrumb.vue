<template>
	<div v-if="isShowBreadcrumb" class="layout-navbars-breadcrumb">
		<SvgIcon class="layout-navbars-breadcrumb-icon --static" name="ele-Fold" :size="16" />
		<el-breadcrumb v-if="state.breadcrumbList.length" class="layout-navbars-breadcrumb-hide">
			<transition-group name="breadcrumb">
				<el-breadcrumb-item v-for="(v, k) in state.breadcrumbList" :key="!v.meta.tagsViewName ? v.meta.title : v.meta.tagsViewName">
					<span v-if="k === state.breadcrumbList.length - 1" class="layout-navbars-breadcrumb-span">
						<SvgIcon v-if="themeConfig.isBreadcrumbIcon && v.meta.icon" :name="v.meta.icon" class="layout-navbars-breadcrumb-iconfont" />
						<div v-if="!v.meta.tagsViewName">{{ $t(<string>v.meta.title) }}</div>
						<div v-else>{{ v.meta.tagsViewName }}</div>
					</span>
					<a v-else @click.prevent="onBreadcrumbClick(v)">
						<SvgIcon v-if="themeConfig.isBreadcrumbIcon && v.meta.icon" :name="v.meta.icon" class="layout-navbars-breadcrumb-iconfont" />
						{{ $t(<string>v.meta.title) }}
					</a>
				</el-breadcrumb-item>
			</transition-group>
		</el-breadcrumb>
	</div>
</template>

<script setup lang="ts" name="layoutBreadcrumb">
import { reactive, computed, onMounted, watch } from 'vue';
import { onBeforeRouteUpdate, useRoute, useRouter, type RouteLocationNormalizedLoaded } from 'vue-router';
import other from '/@/utils/other';
import { storeToRefs } from 'pinia';
import { useThemeConfig } from '/@/stores/themeConfig';

// 主题配置 Store
const storesThemeConfig = useThemeConfig();
const { themeConfig } = storeToRefs(storesThemeConfig);
const route = useRoute();
const router = useRouter();
const state = reactive<BreadcrumbState>({
	breadcrumbList: [],
});

// 根据配置判断是否展示面包屑
const isShowBreadcrumb = computed(() => {
	const { layout, isBreadcrumb } = themeConfig.value;
	if (layout === 'classic' || layout === 'transverse') return false;
	return Boolean(isBreadcrumb);
});
// 面包屑点击跳转
const onBreadcrumbClick = (v: RouteItem) => {
	const { redirect, path } = v;
	if (redirect) router.push(redirect);
	else router.push(path);
};
// 依据当前路由生成面包屑列表
const buildBreadcrumbList = (currentRoute: RouteLocationNormalizedLoaded) => {
	if (!themeConfig.value.isBreadcrumb) {
		state.breadcrumbList = [];
		return;
	}
	const matched = currentRoute.matched.filter((item) => item.meta?.title && !item.meta?.isHide);
	const list = matched.map((item) => {
		const breadcrumbItem: RouteItem = {
			path: item.path,
			name: item.name as any,
			redirect: typeof item.redirect === 'string' ? item.redirect : undefined,
			meta: { ...item.meta },
			children: (item.children || []) as any,
		};
		return breadcrumbItem;
	});
	if (list.length > 0) {
		const lastIndex = list.length - 1;
		list[lastIndex].meta!.tagsViewName = other.setTagsViewNameI18n(<RouteToFrom>currentRoute);
	}
	state.breadcrumbList = list;
};
// 组件挂载时初始化面包屑
onMounted(() => {
	buildBreadcrumbList(route);
});
// 路由变更时同步面包屑
onBeforeRouteUpdate((to) => {
	buildBreadcrumbList(to);
});

watch(
	() => [themeConfig.value.isBreadcrumb, themeConfig.value.layout, route.fullPath],
	() => {
		buildBreadcrumbList(route);
	}
);
</script>

<style scoped lang="scss">
.layout-navbars-breadcrumb {
	flex: 1;
	height: inherit;
	display: flex;
	align-items: center;
	.layout-navbars-breadcrumb-icon {
		cursor: default;
		font-size: 18px;
		color: var(--next-bg-topBarColor);
		height: 100%;
		width: 40px;
		opacity: 0.8;
		&:hover {
			opacity: 1;
		}
	}
	.layout-navbars-breadcrumb-span {
		display: flex;
		opacity: 0.7;
		color: var(--next-bg-topBarColor);
		gap: 6px;
		align-items: center;
	}
	.layout-navbars-breadcrumb-iconfont {
		font-size: 14px;
		margin-right: 0;
		color: inherit;
	}
	:deep(.el-breadcrumb__separator) {
		opacity: 0.7;
		color: var(--next-bg-topBarColor);
	}
	:deep(.el-breadcrumb__inner a, .el-breadcrumb__inner.is-link) {
		font-weight: unset !important;
		color: var(--next-bg-topBarColor);
		display: inline-flex;
		align-items: center;
		gap: 6px;
		&:hover {
			color: var(--el-color-primary) !important;
		}
	}
}
.layout-navbars-breadcrumb-icon.--static {
	pointer-events: none;
	opacity: 0.6;
}
</style>
