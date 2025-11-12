# 后端子系统详解（yolo_cropDisease_detection_springboot）

> 本文档面向 Java/Spring 工程师，阐述业务中台的架构、模块职责、接口、数据模型与部署要点。

## 1. 基础信息
- **框架**：Spring Boot 2.x、MyBatis-Plus、Lombok、Maven Wrapper。
- **运行环境**：JDK 8+（已通过 Java 17 验证），依赖 MySQL 8、可选 Redis。
- **构建**：`./mvnw -DskipTests package`，或 `./mvnw spring-boot:run` 直接运行。
- **配置文件**：`src/main/resources/application.properties`，集中管理数据源、Flask Base URL、天气参数、MQTT（预留）。

## 2. 包结构
```
com.example.Kcsj
├─ bootstrap/            # 启动初始化，如 CommandLineRunner
├─ common/               # 统一返回 Result、异常处理
├─ config/               # CORS、Swagger、MQTT（预留）等配置
├─ controller/           # 14+ 领域控制器（用户、任务、传感器等）
├─ dto/                  # SolutionRecommendation、WeatherData、TaskCreationDTO 等
├─ entity/               # MyBatis-Plus 实体映射病害知识、方案、记录、传感器
├─ mapper/               # Mapper 接口（自动生成或手写 SQL）
├─ service/ & impl/      # 业务服务层
└─ KcsjApplication       # 入口类
```

## 3. 核心模块
### 3.1 用户与权限
- `UserController`：登录、注册、分页查询、增删改等常用接口。
- 认证方式为 Token（暂存 Session），建议生产接入 JWT + RBAC。

### 3.2 识别记录与文件
- `ImgRecordsController` / `VideoRecordsController` / `CameraRecordsController`：对 `imgrecords`、`videorecords`、`camerarecords` 表执行 CRUD，支持分页模糊搜索。
- `FileController`：`/files/upload`、`/files/{flag}` 文件上传下载，富文本上传走 `/files/editor/upload` 返回 `wangeditor` 期望格式。

### 3.3 推理编排（`PredictionController`）
- `POST /flask/predict`：
  1. 校验参数（图片 URL、本地图、权重、作物类别、阈值）。
  2. 转发至 Flask `http://localhost:5001/predictImg`，等待 JSON 结果。
  3. 写入 `imgrecords`（包含 outImg、confidence、label、allTime、taskId）。
  4. 解析标签 → `DiseaseMapperService` 映射到 `diseaseId`，根据 `kind` 反推出 `cropId`。
  5. 读取气象缓存 `WeatherService.getDefaultWeatherSnapshot()`，调用 `SolutionService.generateSolution()` 产出 `SolutionRecommendation`。
  6. 将方案结构化数据与时间窗口、限制条件注入响应，供前端展示或落地任务。
- `GET /flask/file_names`：代理 Flask `/file_names`，列出可选权重。
- 任务事件：通过 SocketIO（Flask 负责推送）即可同步到前端。

### 3.4 防治方案与任务
- `SolutionController`：
  - `GET /solution/catalog`：查询 `tb_solution_plan` 有效方案，返回病害/作物/药剂组合。
  - `POST /solution/generate`：支持前端传入 `solutionId` 或 `diseaseId/cropId` + 坐标，内部调用 `SolutionService.generateSolution` 并可指定气象坐标（调用 `WeatherService.refreshForecast`）。
  - `POST /solution/apply`：将方案绑定识别记录并通过 `TaskService` 创建农事任务。
- `TaskController`：提供列表、详情、创建、更新、删除、状态切换；支持根据执行人、优先级、时间范围查询。

### 3.5 IoT 传感器
- `SensorController`：
  - `POST /sensor/data|upload|batch`：保存单条或批量传感器数据；若未指定 `deviceId` 使用 `sensor.default-device-id`。
  - `GET /sensor/latest|history|devices`：查询最新/历史数据与设备列表。默认历史范围为近 7 天，可指定 `startTime/endTime`。
- `SensorService`：校验数据完整性，写入 `sensor_data` 表，并提供按设备/时间范围查询的方法。
- 结合硬件 TODO 文档，可引入 MQTT Listener 将 `device/sensor/upload/+` Topic 数据转化为 `SensorData`，并实现 `/device/control` 命令转发。

### 3.6 知识库
- `DiseaseInfoController` / `RemedyController`：基于 `tb_disease_info`、`tb_remedy` 提供查询、创建、导入等接口。
- `SolutionPlanController`：维护 `tb_solution_plan`（状态 ACTIVE/INACTIVE、推荐剂量、成本预估、气象限制）。
- `DiseaseMapperService`：封装 Flask 标签与 `diseaseId` 的映射逻辑，可扩展自定义字典。

### 3.7 天气服务
- `WeatherController`：`GET /weather/current`、`POST /weather/refresh`，前者读缓存，后者强制刷新指定坐标。
- `WeatherServiceImpl`：基于 Open-Meteo API，缓存 30 分钟，`@Scheduled` 定时刷新默认坐标；解析小时/天级数据生成 `WeatherData`（含未来 7 天预测、降雨概率、风速、日照等）。

### 3.8 聊天与辅助模块
- `ChatController`：预留 HTTP/WebSocket 接口，可对接大模型或企业 IM。
- `DeviceController`：接口占位，待硬件指令下发功能接入。

## 4. 数据层
- 全部实体位于 `entity/`，采用 MyBatis-Plus 注解映射。
- SQL 初始化脚本：
  - `disease_knowledge_data.sql`：病害基本信息。
  - `remedy_solution_seed.sql`：方案/药剂/成本样例。
  - `hardware_tables.sql`、`remedy_solution_seed.sql` 等补齐传感器、任务、解决方案。
- 统一返回体 `Result<T>` 位于 `common/Result.java`，`Result.success(data)`、`Result.error(code,msg)` 形式，前后端一致。

## 5. 与 Flask 的接口契约
| 步骤 | 内容 |
| --- | --- |
| 1 | 前端调用 `/flask/predict`，中台封装 `PredictRequest`（username/startTime/weight/inputImg/kind/conf/taskId）。 |
| 2 | RestTemplate 将请求 JSON 转发至 Flask `/predictImg`（5001）。 |
| 3 | Flask 返回 `{status,outImg,confidence,label,allTime}`；若 status=400 则中台直接返回 error。 |
| 4 | 成功时，写入 `ImgRecords`，并尝试生成 `SolutionRecommendation`，与 Flask 原始响应一起返回。 |
| 5 | 若 `taskId` 存在，则前后端均可根据 `ImgRecords.taskId` 将记录挂载在农事任务上。

## 6. 配置与运行
1. **数据库**：创建 `cropdisease` 库，导入 SQL。调整 `spring.datasource.username/password`。
2. **Flask 服务地址**：`flask.base-url`（示例 `http://localhost:5001`）。
3. **天气**：`weather.default-latitude/longitude` 指定默认地块位置；`weather.cache-ttl-minutes`、`weather.refresh-interval-ms` 控制缓存和刷新频率。
4. **传感器默认设备**：`sensor.default-device-id=DEVICE_001`。
5. **日志**：使用 `@Slf4j` 记录关键流程、异常，建议结合 `logging.file.name` 输出到文件。

## 7. 性能与可靠性建议
- 使用 `@Transactional` 包裹批量写入与方案生成，避免半写入状态。
- 对天气、方案等静态数据使用缓存（当前在 `WeatherService` 已实现）；后续可为 `SolutionCatalog` 增加本地缓存。
- 识别请求建议加入限流（如基于 Bucket4j）以保护 Flask 服务。
- 可扩展异步事件（Spring ApplicationEvent/消息队列）在记录写入后触发通知或任务创建。

## 8. 硬件集成路线
- `“农疾智判”项目硬件集成 TODO 文档.md` 已规划：
  - 扩展 `SensorData` 字段匹配硬件采集（温度、光照、水位等）。
  - `pom.xml` 新增 `spring-integration-mqtt`、`paho` 依赖，编写 `MqttConfig` + `InboundMessageHandler`，订阅 `device/sensor/upload/+`。
  - 新增控制接口 `/device/control/{deviceId}`，将任务/方案生成的动作（如灌溉、通风、报警）投递到 MQTT Topic `device/control/{deviceId}`。

## 9. 测试与排障
- **单元测试**：尚未覆盖关键模块，建议优先为 `SolutionService`、`SensorService`、`WeatherService` 编写测试用例。
- **排障步骤**：
  1. 查看 Spring Boot 控制台日志（重点关注 `PredictionController`、`SolutionServiceImpl`）。
  2. 检查数据库是否写入记录；若无记录但 Flask 已返回，说明 Mapper/事务存在异常。
  3. 测试 `/sensor/latest` 是否有数据；无数据则检查硬件上传或 Postman 手动写入。
  4. 若天气刷新失败，确认服务器是否能访问 `https://api.open-meteo.com`，必要时配置代理。

## 10. 二次开发建议
- 引入 Swagger/OpenAPI：在 `config/SwaggerConfig` 启用 Knife4j，便于接口自发现。
- 分表/分库：针对 `imgrecords` 等高写入表可启用分区或冷热分离。
- 审计与告警：给关键操作写 `@ControllerAdvice` 记录审计日志，并在任务超时/病害高风险时推送钉钉/企业微信。

---
本说明可配合数据库 ER 图、时序图进一步完善，确保团队成员对后端逻辑有统一认知。
