# 前端子系统详解（yolo_cropDisease_detection_vue）

> 本文面向前端与全栈工程师，全面拆解可视化子系统的技术栈、目录结构、业务模块以及调试部署方式。

## 1. 技术栈与基础设施
- **运行框架**：Vue 3 + Vite 4 + TypeScript，组合 Element Plus UI、Pinia 状态管理、Vue Router 4、Vue I18n 9。
- **可视化**：ECharts（含 GL / LiquidFill / WordCloud）驱动监测大屏，Vue Grid Layout 实现拖拽式卡片布局，CountUp/Seamless Scroll 丰富动效。
- **实时与多媒体**：`socket.io-client` 与 `stompjs`/`sockjs-client` 支撑识别任务进度、聊天等实时能力；`cropperjs`、`wangeditor` 用于图像裁剪与富文本。
- **构建规范**：`package.json` 提供 `vite --force` 开发、`vite build` 产物构建、`eslint --fix` 统一代码风格；Node ≥16 + npm ≥7。

## 2. 目录结构与执行入口
```
src/
├─ main.ts                # 创建应用，注册 Pinia / Router / I18n / ElementPlus
├─ layout/                # 基于 next-admin 的多标签页后台布局
├─ router/                # 静态/动态路由拆分，frontEnd.ts + backEnd.ts
├─ stores/                # Pinia 模块：用户态、标签页、主题、路由缓存等
├─ api/                   # axios 请求封装，按领域拆分（sensor、solution 等）
├─ views/                 # 业务页面：识别、方案、传感器、任务、聊天等
├─ utils/                 # request/session、格式化、SVG 注入
└─ theme/                 # Element Plus 主题变量、暗色/宽屏样式
```
- `src/main.ts`（入口）完成 SVG 图标注册、国际化注入、`VueGridLayout` 注册，确保大屏布局即取即用。
- `src/utils/request.ts` 包装 Axios，自动携带 Session token，401/4001 时触发重登提示；全局超时 50s 并内建 `ElMessage` 错误提醒。

## 3. 路由与权限
- 路由配置拆分为 `frontEnd.ts`（静态）与 `backEnd.ts`（基于服务端 menu 的动态路由），`src/stores/routesList.ts` 负责根据用户角色构造侧边栏。
- 登录态通过 `src/views/login/index.vue` 调用 `/user/login`，凭证写入 `Session`（localStorage 封装），刷新后由 `router.beforeEach` 自动还原路由/标签。

## 4. 状态管理
- `stores/userInfo.ts`：存储用户详情、token、权限列表，封装登录/登出行为。
- `stores/themeConfig.ts`：提供导航模式、面包屑、色板等可配置项，与 Element Plus 主题变量联动。
- `stores/tagsViewRoutes.ts`：多标签页缓存，结合 `keepAliveNames` 提升频繁切换性能。

## 5. 网络层与 API 约定
- 所有 API 置于 `src/api/*`，方法只负责描述 URL、method、params/body，统一触发 Axios 实例；如 `sensor.ts` 暴露 `fetchSensorDevices/latest/history/summary/trend`。
- 上传走 `/api/files/upload`，由 `el-upload` 组件承载，结果写入表单状态供识别或任务模块复用。
- WebSocket 触达：`socket.io-client` 连接 Flask 服务，订阅 `processing/completed/failed` 事件，在识别记录或任务详情弹出实时提示。

## 6. 核心业务页面
1. **图像识别（`src/views/imgPredict`）**
   - 参数面板支持作物类型、权重模型、置信度阈值设置；权重列表通过 `/flask/file_names` 动态读取。
   - 上传组件内嵌预览/拖拽，触发 `upData()` 时调用 `/flask/predict`，等待响应后展示标签/置信度/耗时，并提供「查看推荐防治方案」跳转。
2. **视频 / 摄像头识别**
   - `src/views/videoPredict`、`cameraPredict` 调用 Flask `/predictVideo`、`/predictCamera`，通过进度条+日志流展示处理状态，输出视频存储路径并记录到 `videorecords/camerarecords`。
3. **识别记录（`imgRecord`、`videoRecord`、`cameraRecord`）**
   - 统一表格组件 + 搜索条件 + 导出功能，便于运维回溯。
4. **智能方案中心（`src/views/solution`）**
   - 方案参数区：选择病害/作物/药剂组合（`/solution/catalog`）、指定坐标，点击生成即调用 `/solution/generate`。
   - 推荐区：结构化展示目标病害、药剂、剂量、风险提示、成本拆解、气象建议、时间窗口、限制条件，可直接落地任务。
5. **传感器监控（`src/views/sensor`）**
   - 顶部动作支持手动刷新、设备切换、30s 自动轮询；中间卡片实时展示温度/湿度/光照/水位/CO₂，表格记录最新 50 条历史数据。
   - 底部趋势图使用 ECharts 复合折线/面积图，根据「近 1/3/7 天」切换调用 `/sensor/history`。
6. **知识库（`knowledge`、`encyclopedia`）**
   - 结合 `disease`/`remedy` 接口，支持查询病害百科、药剂详情、富文本展示典型症状与治理要点。
7. **任务与协同（`src/views/task`）**
   - 列表 + 卡片 + 甘特视图（依赖 `vue-grid-layout`），可关联识别记录编号、负责人、优先级，状态颜色提示。
8. **聊天、设备控制、个人中心**
   - `chat` 页面通过 WebSocket 接入 LLM 服务（预留）；`deviceControl`（位于仓库根 `src/views/deviceControl`）展示 IoT 在线状态与继电器操作面板；`personal` 支持修改头像、密码、通知偏好。

## 7. 设计细节与交互亮点
- **统一反馈**：所有表单操作使用 Element Plus `ElMessage`/`ElMessageBox`，结合 loading 状态避免重复提交。
- **响应式布局**：`sensor`、`solution` 页面大量使用 `el-row/el-col` 与 CSS Grid，保证在 1366 宽屏与 4K 大屏下均有良好展示。
- **国际化**：`src/i18n` 保留多语言能力，默认中文，可按需扩展。
- **动画与大屏**：`animate.css`、`countup.js` 提供卡片数字增长、组件入场动画，增强数据大屏表现力。

## 8. 本地开发与部署
1. `npm install`
2. `npm run dev`（或 `pnpm dev`）
3. 在 `.env.development` 配置：
   ```env
   VITE_API_URL=http://localhost:9999
   VITE_SOCKET_URL=http://localhost:5001
   ```
4. 生产构建：`npm run build` → `dist/` 产物可由 Nginx/Gateway 提供；推荐启用 gzip 与缓存头。

## 9. 调试建议
- 使用 VS Code Volar + TypeScript 插件获取强提示。
- API 报错时查看浏览器 Network + 后端日志，同时确认 `Session` 是否过期（401 自动跳转登录）。
- 对实时模块，可在浏览器控制台监听 `socket.on` 事件，排查推理/任务状态未更新问题。

## 10. 扩展与二开方向
- 接入多租户：基于路由守卫与 Pinia 扩充 `tenantId`，在请求头中附带，后端按租户隔离数据。
- 大屏模式：利用 `VueGridLayout` 保存布局 JSON，在数据库或本地缓存中存档，实现“自定义监控屏”。
- PWA/离线：借助 Vite 插件构建 PWA，关键数据写入 IndexedDB，支持断网情况下浏览知识库或历史记录。

如需更详细的组件 API 或状态图，可继续基于本文档输出 Storybook/设计指南。
