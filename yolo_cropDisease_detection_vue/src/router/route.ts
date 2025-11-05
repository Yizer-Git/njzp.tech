import { RouteRecordRaw } from 'vue-router';

/**
 * 说明：路由 path 的文件命名需与真实文件目录保持一致，便于定位。
 *
 * 路由 meta 字段用于控制菜单展示：
 * meta: {
 *      title:          菜单与 tagsView 展示名
 *      isLink:         外链地址
 *      isHide:         是否在菜单中隐藏
 *      isKeepAlive:    是否缓存
 *      isAffix:        是否固定在 tagsView
 *      isIframe:       是否 iframe 嵌套
 *      roles:          哪些角色可见，默认全部
 *      icon:           菜单与 tagsView 图标
 * }
 */

// 扩展 RouteMeta 接口
declare module 'vue-router' {
	interface RouteMeta {
		title?: string;
		isLink?: string;
		isHide?: boolean;
		isKeepAlive?: boolean;
		isAffix?: boolean;
		isIframe?: boolean;
		roles?: string[];
		icon?: string;
	}
}

/**
 * 动态路由：前端静态路由版本，当 isRequestRoutes=false 时使用。
 */
export const dynamicRoutes: Array<RouteRecordRaw> = [
	{
		path: '/',
		name: '/',
		component: () => import('/@/layout/index.vue'),
		redirect: '/detection/imgPredict',
		meta: {
			isKeepAlive: true,
		},
		children: [
			{
				path: '/detection',
				name: 'detection',
				component: () => import('/@/layout/routerView/parent.vue'),
				redirect: '/detection/imgPredict',
				meta: {
					title: '病害检测',
					isLink: '',
					isHide: false,
					isKeepAlive: true,
					isAffix: false,
					isIframe: false,
					roles: ['admin', 'common', 'others'],
					icon: 'ele-VideoCamera',
				},
				children: [
					{
						path: '/detection/imgPredict',
						name: 'imgPredict',
						component: () => import('/@/views/imgPredict/index.vue'),
						meta: {
							title: '图像检测',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common', 'others'],
							icon: 'iconfont icon-Images',
						},
					},
					{
						path: '/detection/videoPredict',
						name: 'videoPredict',
						component: () => import('/@/views/videoPredict/index.vue'),
						meta: {
							title: '视频检测',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common', 'others'],
							icon: 'iconfont icon-Video',
						},
					},
					{
						path: '/detection/cameraPredict',
						name: 'cameraPredict',
						component: () => import('/@/views/cameraPredict/index.vue'),
						meta: {
							title: '摄像检测',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common', 'others'],
							icon: 'iconfont icon-WI-FI',
						},
					},
				],
			},
			{
				path: '/records',
				name: 'records',
				component: () => import('/@/layout/routerView/parent.vue'),
				redirect: '/records/imgRecord',
				meta: {
					title: '识别记录',
					isLink: '',
					isHide: false,
					isKeepAlive: true,
					isAffix: false,
					isIframe: false,
					roles: ['admin', 'common', 'others'],
					icon: 'ele-Collection',
				},
				children: [
					{
						path: '/records/imgRecord',
						name: 'imgRecord',
						component: () => import('/@/views/imgRecord/index.vue'),
						meta: {
							title: '图像识别记录',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common', 'others'],
							icon: 'iconfont icon-Playlist',
						},
					},
					{
						path: '/records/videoRecord',
						name: 'videoRecord',
						component: () => import('/@/views/videoRecord/index.vue'),
						meta: {
							title: '视频识别记录',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common', 'others'],
							icon: 'iconfont icon-Video',
						},
					},
					{
						path: '/records/cameraRecord',
						name: 'cameraRecord',
						component: () => import('/@/views/cameraRecord/index.vue'),
						meta: {
							title: '摄像识别记录',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common', 'others'],
							icon: 'iconfont icon-a-MultipleLinks',
						},
					},
				],
			},
			{
				path: '/solution/plan',
				name: 'solutionPlan',
				component: () => import('/@/views/solution/index.vue'),
				meta: {
					title: '智能防治方案',
					isLink: '',
					isHide: false,
					isKeepAlive: true,
					isAffix: false,
					isIframe: false,
					roles: ['admin', 'common', 'others'],
					icon: 'ele-MagicStick',
				},
			},
			{
				path: '/encyclopedia',
				name: 'encyclopedia',
				component: () => import('/@/views/encyclopedia/index.vue'),
				meta: {
					title: '病害百科',
					isLink: '',
					isHide: false,
					isKeepAlive: true,
					isAffix: false,
					isIframe: false,
					roles: ['admin', 'common', 'others'],
					icon: 'ele-CollectionTag',
				},
			},
			{
				path: '/knowledge',
				name: 'knowledge',
				redirect: '/knowledge/disease',
				meta: {
					title: '知识库管理',
					isLink: '',
					isHide: false,
					isKeepAlive: true,
					isAffix: false,
					isIframe: false,
					roles: ['admin'],
					icon: 'ele-Reading',
				},
				children: [
					{
						path: '/knowledge/disease',
						name: 'diseaseManage',
						component: () => import('/@/views/knowledge/disease/index.vue'),
						meta: {
							title: '病害管理',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin'],
							icon: 'ele-Warning',
						},
					},
					{
						path: '/knowledge/remedy',
						name: 'remedyManage',
						component: () => import('/@/views/knowledge/remedy/index.vue'),
						meta: {
							title: '药剂管理',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin'],
							icon: 'ele-Medicine',
						},
					},
					{
						path: '/knowledge/solution',
						name: 'solutionManage',
						component: () => import('/@/views/knowledge/solution/index.vue'),
						meta: {
							title: '方案管理',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin'],
							icon: 'ele-Document',
						},
					},
				],
			},
			{
				path: '/task',
				name: 'task',
				component: () => import('/@/views/task/index.vue'),
				meta: {
					title: '农事计划',
					isLink: '',
					isHide: false,
					isKeepAlive: true,
					isAffix: false,
					isIframe: false,
					roles: ['admin', 'common'],
					icon: 'iconfont icon-Events',
				},
			},
			{
				path: '/usermanage',
				name: 'usermanage',
				component: () => import('/@/views/userManage/index.vue'),
				meta: {
					title: '用户管理',
					isLink: '',
					isHide: false,
					isKeepAlive: true,
					isAffix: false,
					isIframe: false,
					roles: ['admin'],
					icon: 'iconfont icon-a-BusinessCard',
				},
			},
			{
				path: '/personal',
				name: 'personal',
				component: () => import('/@/views/personal/index.vue'),
				meta: {
					title: '个人中心',
					isLink: '',
					isHide: false,
					isKeepAlive: true,
					isAffix: false,
					isIframe: false,
					roles: ['admin', 'common', 'others'],
					icon: 'iconfont icon-Feedback',
				},
			},
			{
				path: '/assistant',
				name: 'assistant',
				component: () => import('/@/views/chat/index.vue'),
				meta: {
					title: '智能助手',
					isLink: '',
					isHide: false,
					isKeepAlive: true,
					isAffix: false,
					isIframe: false,
					roles: ['admin', 'common', 'others'],
					icon: 'ele-ChatSquare',
				},
			},
		],
	},
];

/**
 * 404 / 401 固定路由
 */
export const notFoundAndNoPower = [
	{
		path: '/:path(.*)*',
		name: 'notFound',
		component: () => import('/@/views/error/404.vue'),
		meta: {
			title: 'message.staticRoutes.notFound',
			isHide: true,
		},
	},
	{
		path: '/401',
		name: 'noPower',
		component: () => import('/@/views/error/401.vue'),
		meta: {
			title: 'message.staticRoutes.noPower',
			isHide: true,
		},
	},
];

/**
 * 静态路由：无需权限控制的基础路由
 */
export const staticRoutes: Array<RouteRecordRaw> = [
	{
		path: '/login',
		name: 'login',
		component: () => import('/@/views/login/index.vue'),
		meta: {
			title: '登录',
		},
	},
	{
		path: '/register',
		name: 'register',
		component: () => import('/@/views/login/register.vue'),
		meta: {
			title: '注册',
		},
	},
	{
		path: '/videoShow',
		name: 'videoShow',
		component: () => import('/@/views/videoRecord/show.vue'),
		meta: {
			title: '录像查看',
		},
	},
	/**
	 * 其余动态路由请写在 dynamicRoutes 中
	 */
];













