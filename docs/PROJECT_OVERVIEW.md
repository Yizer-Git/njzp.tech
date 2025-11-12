# 农疾智判项目总体说明

> 本文面向产品、研发与运维团队，系统性阐述农疾智判平台的目标定位、系统边界、运行依赖以及跨端协作方式，可作为立项答辩、交付验收或二次开发的权威说明文档。

## 1. 业务场景与价值
- **定位**：以“感知 → 诊断 → 决策 → 执行”为主线的作物病害智能诊断与农情决策平台，支撑农业经营主体在多地块、多作物场景中实现快速预警与闭环治理。
- **目标用户**：种植大户、农业服务公司、合作社技术员以及政府农技推广人员。
- **核心痛点**：病虫害识别效率低、方案依赖专家、现场执行缺少跟踪、软硬件割裂导致数据孤岛。平台通过多模态识别、知识图谱、任务协同和 IoT 集成形成统一“数字农服”入口。

## 2. 整体架构
```
┌──────────────────┐       ┌────────────────────┐
│  Vue3 前端可视化   │ REST │ Spring Boot 业务中台 │───MyBatis──▶ MySQL/Redis
│· 识别、方案、任务   │◀────│· 用户、知识、方案    │
│· IoT 监控、大屏     │  WS │· 调用 Flask/LLM/天气 │
└──────────△───────┘       └───────────△────────┘
             │ HTTP 文件网关                 │HTTP/SocketIO
             │                                │
             │                                ▼
             │                    ┌──────────────────────┐
             │                    │ Flask + YOLO 推理服务 │
             │                    │· 图像/视频/相机推理    │
             │                    │· 模型管理与任务事件   │
             │                    └──────────────────────┘
             │
             ▼
   智能硬件（传感器/继电器）
```
- **三端解耦**：前端负责体验与实时可视化，中台负责数据/业务编排，算法服务负责推理供给，松耦合 REST/SocketIO 机制确保可独立扩缩容。
- **数据通路**：IoT 设备经 MQTT/HTTP 写入 `sensor_data` 表 → 中台提供 `/api/sensor/*` 查询 → 前端看板可视化；图像识别请求从前端表单→中台 `/flask/predict`→Flask `/predictImg`→识别结果写入 `imgrecords`，并可反向触发方案生成及任务落地。

## 3. 代码与目录总览
| 层 | 目录 | 说明 |
| --- | --- | --- |
| 前端 | `yolo_cropDisease_detection_vue/` | Vite + Vue3 + Element Plus，含传感器监控、识别中心、方案中心、任务、知识库、聊天等视图。 |
| 后端 | `yolo_cropDisease_detection_springboot/` | Spring Boot 微服务，聚合用户/权限、病害知识、任务流、传感器、文件、天气和 AI 协同。 |
| 算法 | `yolo_cropDisease_detection_flask/` | Flask + Ultralytics YOLO + SocketIO，提供图片、视频、摄像头识别及模型热加载。 |
| 数据脚本 | `disease_knowledge_data.sql`、`remedy_solution_seed.sql` 等 | 初始化病害知识、方案、任务、传感器示例数据。 |
| 硬件规划 | `“农疾智判”项目硬件集成 TODO 文档.md` | 将嵌入式终端通过 MQTT 与项目对接的详细任务清单。 |

## 4. 关键能力串联
1. **病害识别闭环**
   - 前端 `src/views/imgPredict` 负责上传、参数选择、阈值调节与结果展示。
   - 中台 `PredictionController` 校验请求、转发 Flask、落库识别记录，并联动 `SolutionService` 自动生成防治方案与推荐时间窗。
   - Flask 服务 `ImagePredictor` 管理多作物标签集，支持远程图片下载、GPU/FP16 推理、结果回传与 SocketIO 进度事件。
2. **知识与方案库**
   - `tb_disease_info`、`tb_remedy`、`tb_solution_plan` 三表承载病害知识、药剂经济性与作业模板。
   - `/solution/generate` 结合传感器最新值与 `WeatherService` 缓存的 Open‑Meteo 气象，实现智能化作业建议。
3. **农事任务协同**
   - `/task` 系列接口支持创建、分配、状态流转，并可由防治方案直接落地任务（`SolutionService.applySolution`）。
   - 前端 `src/views/task` 提供列表/看板/甘特图等视图，关联识别记录或方案。
4. **IoT 感知扩展**
   - `SensorController` 支持单条、批量写入以及设备列表查询；`sensor` 前端页面提供实时卡片、历史表格与趋势图。
   - 硬件文档规划 MQTT Topic（`device/sensor/upload/+`、`device/control/+`），后端可通过 Spring Integration MQTT 实现命令下发与数据回传。

## 5. 部署拓扑与环境要求
| 组件 | 默认端口 | 依赖 | 备注 |
| --- | --- | --- | --- |
| Vue 前端 | 8080 | Node.js ≥16 | `.env` 中需配置 `VITE_API_URL` 指向网关。 |
| Spring Boot | 9999 | MySQL 8、可选 Redis | `application.properties` 配置数据源、Flask/LLM/天气参数。支持 `mvnw spring-boot:run`。 |
| Flask 推理 | 5001 | Python 3.10 + CUDA (可选) | `pip install -r requirements.txt`，GPU 自动启用 FP16，WebSocket 默认开放。 |
| 数据库 | 3306 | MySQL | 导入根目录 SQL，或使用 `docker-compose` 示例拉起。 |

> 建议在开发环境将三端独立运行，生产可拆分为：Nginx 静态托管前端、K8s StatefulSet 跑 Spring Boot、GPU 节点运行 Flask 推理。

## 6. 配置要点
- **Spring Boot**：
  - `spring.datasource.*` 指定数据库；`flask.base-url` 指向推理服务；`weather.*` 控制默认坐标、缓存 TTL、定时刷新。
  - `sensor.default-device-id` 和 `sensor.batch-limit` 控制 IoT 默认映射。
- **前端**：
  - `src/utils/request.ts` 读取 `VITE_API_URL`，Session 存储 token 并自动在 401 时跳转登录。
  - `.env.development`、`.env.production` 可分别配置 API、WebSocket、文件 CDN。
- **Flask**：
  - `ENABLE_REMOTE_UPLOAD` 控制是否将识别结果推送到对象存储；`weights/` 内可放多套权重，通过 `/file_names` 自动读取。
  - `runs/` 目录用于缓存推理日志与临时下载，需确保磁盘可写。

## 7. 数据与安全
- 所有 API 统一返回 `Result{code,msg,data}`，401/4001 会触发前端 Session 清理。
- 上传文件走 `/files/upload`，使用随机 `flag` 映射磁盘路径，保障直接访问路径不可猜测。
- 识别记录、方案建议、任务数据存在显式关联字段，可用于审计与追溯。
- 建议生产环境：
  - 使用 HTTPS + WebSocket over TLS。
  - 将明文密码迁移到 bcrypt，并引入 JWT/Token 过期策略。
  - 借助 Redis/消息队列缓冲高并发识别任务。

## 8. 运维与观测
- Spring Boot 侧通过 `Slf4j` 打日志，可融合 ELK；`WeatherService` 使用 `@Scheduled` 定时刷新气象数据。
- Flask 侧 SocketIO 事件（`processing/completed/failed`）可直接喂给前端或运维看板。
- 通过数据库中的 `imgrecords/videorecords/camerarecords` 表可统计推理成功率与耗时，结合 `sensor_data` 进行关联分析。

## 9. 未来迭代路线
1. **硬件闭环**：按照硬件 TODO 文档完成 MQTT Collector 与 Modbus/继电器驱动，实现阈值触发自动控水、控风、报警。
2. **算法迭代**：引入多作物多模型编排、端云协同、蒸馏/量化推理以适配边缘设备。
3. **多租户与权限**：细化到地块/组织维度的 RBAC，结合地图可视化展示任务与风险热力。
4. **大模型助手**：在 `/chat` 模块接入国产大模型，结合知识库提供自然语言问诊与操作指南。
5. **DevOps**：完善 CI/CD（Lint + 单测 + docker build），通过 Helm Chart 发布并引入灰度策略。

---
如需进一步的 SRS/接口文档，可继续细化至 swagger/openapi、Sequence Diagram 或测试用例文档。
