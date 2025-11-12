# 算法与推理子系统详解（yolo_cropDisease_detection_flask）

> 聚焦于 Flask + YOLO 推理服务的整体设计、接口契约、模型管理、视频/摄像头处理、实时事件以及部署调优。

## 1. 技术栈
- **框架**：Flask 2 + Flask-SocketIO 5（基于 eventlet/WSGI），REST + WebSocket 双通道。
- **深度学习**：Ultralytics YOLO v8 系列，Torch 1.9+，支持 CUDA/FP16 自动启用。
- **多媒体**：OpenCV 4 处理视频帧，NumPy 进行矩阵运算。
- **任务管理**：自研 `ImagePredictor`，封装模型加载、标签映射、推理耗时统计与结果保存。

## 2. 项目结构
```
yolo_cropDisease_detection_flask/
├─ main.py               # VideoProcessingApp：Flask 入口、路由、SocketIO
├─ predict/
│  └─ predictImg.py      # ImagePredictor 类，封装图像推理
├─ weights/              # 存放 *.pt 权重，可自定义命名
├─ runs/
│  ├─ temp/              # 远程图片/视频临时下载目录
│  └─ video/             # 视频处理缓存
├─ static/results/       # 输出图片/视频供前端访问
├─ requirements.txt      # 依赖清单
t└─ train.py             # 预留训练脚本入口
```

## 3. Flask 应用生命周期
1. **初始化**：`VideoProcessingApp.__init__` 中配置 Flask、SocketIO、下载/结果目录、GPU 选择、模型缓存。
2. **路由注册**：
   - `GET /file_names`：读取 `./weights` 下文件，返回 `{value,label}` 数组供前端展示。
   - `POST /predictImg`：图像推理入口。
   - `GET/POST /predictVideo`：视频文件处理，流式读取并在本地写入结果视频。
   - `GET /predictCamera`、`/stopCamera`、`/cameras`：摄像头流接入与控制。
3. **WebSocket 事件**：`connect`/`disconnect`；业务侧通过 `emit_task_event(taskId,status,...)` 把进度更新发送给前端。
4. **运行**：`socketio.run(app, host=0.0.0.0, port=5001, allow_unsafe_werkzeug=True)`。

## 4. 图像推理流程（/predictImg）
1. 前端通过 Spring Boot 转发请求，payload 包含 `username/weight/conf/startTime/inputImg/kind/taskId`。
2. 服务端判断 `inputImg`：
   - 远程 URL：调用 `download()` 保存到 `runs/temp/*`，完成后设置 `cleanup_required=True`。
   - 本地路径：调用 `resolve_local_image_path()` 校验是否存在。
3. 根据 `weight` 调用 `get_or_load_model()`：
   - 若模型已缓存于 `model_cache` 且已 Ready，则复用。
   - 否则加载 `YOLO(weights_path)`，自动放入 GPU/FP16，返回加载耗时。
4. 调用 `ImagePredictor.predict(setup_time)`：
   - `self.labels` 根据 `kind` 选择（如水稻、玉米、草莓、番茄、柑橘），保证标签与训练数据一致。
   - YOLO 推理参数：`conf` 阈值、`half`、`save_conf=True`。
   - 将标签、置信度、耗时写入 `all_results`，并保存绘制后的图片到 `save_path`。
5. 生成结果：
   - 成功：`status=200`，返回 `outImg`（通过 `urljoin` 拼接 `static/results` 路径）、`allTime`、`confidence`、`label`（JSON 字符串），并触发 `emit_task_event(taskId,'completed',...)`。
   - 失败：`status=400`，附带错误信息，触发 `emit_task_event(taskId,'failed',...)` 并清理临时文件。
6. 若开启 `ENABLE_REMOTE_UPLOAD=1`，服务会异步上传识别结果到预设对象存储（可在 `schedule_async_upload` 中扩展 S3/OSS 逻辑）。

## 5. 视频与摄像头推理
- `/predictVideo`：
  - 参数通过 query string 传入，服务端将视频下载至 `runs/video/download.mp4`。
  - 使用 OpenCV 分帧，`model.predict(source=frame)` 实时推理。
  - 将结果写入 `camera_output.avi` 并在处理过程中通过 `yield` + `Response` 推流给前端（若需要）。
  - 结束后触发完成事件，返回输出视频路径。
- `/predictCamera`：
  - 打开摄像头流或 RTSP，设置 `recording=True` 控制循环，`/stopCamera` 将标志改为 False 以停止处理。
  - 支持多摄像头列表 `/cameras`，便于前端选择。

## 6. 模型与性能优化
- **模型缓存**：`self.model_cache` + `self.model_ready` 避免重复加载，`get_or_load_model` 返回模型实例和加载耗时，以便前端显示真实推理时间。
- **GPU/FP16**：`self.device = cuda:0`（若可用），`self.use_half = True`，推理速度更快。
- **远程资源**：支持 HTTP(S) 图片，自动生成随机文件名，推理完成后删除临时文件，防止磁盘膨胀。
- **批量权重**：前端可下拉切换，如 `rice_best.pt`、`corn_best.pt`；也可按需上传轻量模型。

## 7. 事件与任务联动
- `emit_task_event(taskId, status, **payload)`：包装 SocketIO `emit('task_event', {...})`，前端监听以更新识别任务状态。
- 状态枚举：`processing`（开始）、`completed`（成功）、`failed`（异常）。
- 附带字段：用户名、作物类别、输出图片/视频 URL、置信度数组、耗时等，可直接同步到任务列表或通知中心。

## 8. 接口返回示例
```json
{
  "status": 200,
  "message": "预测成功",
  "outImg": "http://localhost:5001/static/results/result_ab12.jpg",
  "allTime": "0.724秒",
  "confidence": "[\"93.15%\",\"88.42%\"]",
  "label": "[\"Rice Blast（稻瘟病）\"]",
  "uploadStatus": "pending"
}
```
异常示例：`{"status":400,"message":"该图片无法识别，请重新上传！"}`。

## 9. 部署与运维
1. **环境**：Python 3.10（建议），安装依赖 `pip install -r requirements.txt`，若使用 GPU 需预装 CUDA/cuDNN。
2. **运行**：`python main.py` 或 `gunicorn main:app`（结合 `eventlet`）。
3. **日志**：目前使用 `print`，建议接入 Python `logging` 并写入文件/ELK。
4. **安全**：配置反向代理（Nginx）限制访问来源，必要时在 Flask 层增加 token 校验，避免未经授权的推理调用。
5. **资源监控**：可借助 `nvidia-smi` 或 Prometheus Node Exporter 监控 GPU/CPU；对 `runs/temp` 定期清理。

## 10. 扩展方向
- **多模型调度**：根据作物或任务优先级选择不同的 YOLO 权重，或引入 ONNX/TensorRT 加速。
- **任务队列**：接入 Celery/RabbitMQ，将推理任务排队执行，提高吞吐并防止阻塞。
- **模型管理 API**：新增 `/weights/upload`、`/weights/switchDefault`，配合数据库记录模型元数据。
- **指标上报**：将推理耗时、识别准确度、错误率上报到 Prometheus，构建可视化看板。
- **边缘节点**：将 `ImagePredictor` 抽象成 gRPC/REST 微服务，在边缘设备部署，中心只负责协调与汇总。

---
结合本说明，可快速定位推理链路问题、扩展新模型并将实时识别能力嵌入业务闭环。
