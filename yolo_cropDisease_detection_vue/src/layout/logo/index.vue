<template>
	<div class="layout-logo">
		<img :src="logoMini" class="layout-logo__logo" />
		<div class="layout-logo__meta">
			<span class="layout-logo__title">{{ themeConfig.globalTitle }}</span>
			<span class="layout-logo__subtitle">Data Management Control</span>
		</div>
	</div>
</template>

<script setup lang="ts" name="layoutLogo">
import { storeToRefs } from 'pinia';
import { onMounted } from 'vue';
import { useThemeConfig } from '/@/stores/themeConfig';
import logoMini from '/@/assets/logo-mini.svg';

const storesThemeConfig = useThemeConfig();
const { themeConfig } = storeToRefs(storesThemeConfig);

onMounted(() => {
	if (themeConfig.value.isCollapse) {
		themeConfig.value.isCollapse = false;
	}
});
</script>

<style scoped lang="scss">
.layout-logo {
	width: 100%;
	height: 64px;
	display: flex;
	align-items: center;
	justify-content: flex-start;
	padding: 0 18px;
	gap: 12px;
	user-select: none;
	&__logo {
		width: 40px;
		height: 40px;
		flex-shrink: 0;
		display: block;
	}
	&__meta {
		display: flex;
		flex-direction: column;
		line-height: 1.1;
		margin-left: 8px;
	}
	&__title {
		font-weight: 700;
		font-size: 18px;
		color: #1aa67f;
	}
	&__subtitle {
		font-size: 11px;
		letter-spacing: 0.4px;
		color: #7c8ba6;
		text-transform: capitalize;
	}
}
</style>
