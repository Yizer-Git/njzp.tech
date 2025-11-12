# 快速启动指南

## 第二阶段：智能防治方案与 LLM 集成

### 前置条件

- Java 1.8+
- Node.js 14+
- MySQL 8.0+
- Python 3.8+ (Flask服务)

---

## 一、数据库初始化

### 1. 导入传感器数据表

```bash
# 连接到MySQL数据库
mysql -u root -p

# 选择数据库
use cropdisease;

# 导入表结构
source sensor_data_schema.sql;

# 导入测试数据（7天历史数据）
source sensor_data_test_data.sql;

# 验证数据
SELECT COUNT(*) FROM tb_sensor_data;
# 应该返回 169 条记录
```

---

## 二、后端启动

### 1. 配置文件检查

确认 `application.properties` 配置正确：

```properties
# 数据库配置
spring.datasource.url=jdbc:mysql://localhost:3306/cropdisease?serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=你的密码

# 讯飞星火配置（可选，暂时使用模拟模式）
llm.spark.app-id=YOUR_APP_ID
llm.spark.api-key=YOUR_API_KEY
llm.spark.api-secret=YOUR_API_SECRET
```

### 2. 启动Spring Boot应用

```bash
cd yolo_cropDisease_detection_springboot

# 方式1：使用Maven
mvn clean install

# 方式2：使用IDE
# 直接运行 com.example.Kcsj.Kcsj 主类
```

**启动成功标志**：
```
Started Kcsj in 15.xxx seconds (JVM running for xx.xxx)
```

**端口**：`http://localhost:9999`

### 3. 启动Flask推理服务（如果未启动）

```bash
cd yolo_cropDisease_detection_flask

#mac进入虚拟环境中
-m venv .venv
source .venv/bin/activate
python main.py
```

**端口**：`http://localhost:5001`

---

## 三、前端启动

```bash
cd yolo_cropDisease_detection_vue

# 安装依赖（首次运行）
npm install

# 启动开发服务器
npm run serve
```

**访问地址**：`http://localhost:8080`

---

## 四、功能测试

### 1. 传感器数据监控

**路径**：`http://localhost:8080/sensor`

**功能**：
- 查看实时环境数据（温度、湿度、土壤墒情、光照、CO2）
- 查看历史趋势图表（可选择1天/3天/7天）
- 手动刷新数据

**API测试**：
```bash
# 查询最新数据
curl http://localhost:9999/api/sensor/latest

# 推送新数据
curl -X POST http://localhost:9999/api/sensor/data \
  -H "Content-Type: application/json" \
  -d '{
    "deviceId": "DEVICE_001",
    "temperature": 25.5,
    "humidity": 68.0,
    "soilMoisture": 45.0,
    "lightIntensity": 35000.0,
    "co2Level": 420.0
  }'
```

### 2. 智能问答助手

**路径**：`http://localhost:8080/chat`

**功能**：
- 实时聊天对话（WebSocket）
- 流式输出（逐字显示）
- 多轮对话（上下文记忆）
- 快捷问题按钮

**快捷问题**：
- "如何防治玉米疫病？"
- "什么时候施药最合适？"
- "如何查看传感器数据？"
- "气象条件对施药有什么影响？"

**API测试**：
```bash
# HTTP方式发送消息
curl -X POST http://localhost:9999/api/chat/send \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "test_session",
    "message": "如何防治玉米疫病？"
  }'
```

### 3. 图像识别 + 智能方案

**路径**：`http://localhost:8080/imgPredict`

**功能**：
- 上传图片识别病害
- 自动生成防治方案
- 推荐施药时间窗口
- 气象因素分析

**测试流程**：
1. 选择作物类型（玉米/水稻/番茄/草莓）
2. 上传病害图片
3. 点击识别按钮
4. 查看识别结果和防治方案

**响应示例**：
```json
{
  "label": ["blight（疫病）"],
  "confidence": ["70.56%"],
  "solutionRecommendation": {
    "diseaseName": "玉米疫病",
    "remedyName": "甲霜·锰锌 72% WP",
    "recommendedDosage": 150.0,
    "recommendedTimeWindows": [
      "今日 16:00-18:00",
      "今日 18:00-20:00",
      "明日 07:00-09:00"
    ],
    "applicationRestrictions": {
      "温度": "当前气温 25.0℃，适宜施药",
      "湿度": "当前湿度 68%，适宜",
      "风速": "当前风速 3.5 m/s，可以施药",
      "降雨": "未来降雨概率 10%，适宜",
      "推荐时段": "早上7-10点或傍晚16-19点"
    }
  }
}
```

---

## 五、LLM真实API接入（可选）

### 1. 获取讯飞星火API密钥

访问：https://xinghuo.xfyun.cn/

注册账号并创建应用，获取：
- AppId
- ApiKey
- ApiSecret

### 2. 修改配置

编辑 `application.properties`：

```properties
llm.spark.app-id=你的AppId
llm.spark.api-key=你的ApiKey
llm.spark.api-secret=你的ApiSecret
llm.spark.wss-url=wss://spark-api.xf-yun.com/v3.5/chat
llm.spark.model-version=v3.5
```

### 3. 重启Spring Boot应用

配置完成后重启应用，系统会自动切换到真实AI模式。

**验证方式**：
- 在聊天界面发送消息
- 查看后端日志，应该显示"调用讯飞星火API"而不是"返回模拟响应"

---

## 六、常见问题

### Q1: 数据库连接失败

**错误**：`Communications link failure`

**解决**：
- 检查MySQL是否启动
- 确认用户名密码正确
- 检查防火墙设置

### Q2: Flask服务无法访问

**错误**：`Connection refused: connect`

**解决**：
```bash
# 确认Flask服务已启动
cd yolo_cropDisease_detection_flask
python main.py

# 检查端口占用
netstat -ano | findstr 5001
```

### Q3: WebSocket连接失败

**错误**：`WebSocket连接失败`

**解决**：
- 检查浏览器是否支持WebSocket
- 清除浏览器缓存
- 确认后端应用正常运行

### Q4: 图表不显示

**解决**：
- 确认已导入测试数据
- 检查浏览器控制台是否有JavaScript错误
- 刷新页面

### Q5: 传感器数据为空

**解决**：
```bash
# 手动导入测试数据
mysql -u root -p cropdisease < sensor_data_test_data.sql

# 验证数据
mysql -u root -p -e "SELECT COUNT(*) FROM cropdisease.tb_sensor_data;"
```

---

## 七、API接口速查

### 传感器数据

| 方法 | 端点 | 说明 |
|------|------|------|
| POST | `/api/sensor/data` | 接收传感器数据 |
| GET | `/api/sensor/latest` | 查询最新数据 |
| GET | `/api/sensor/history` | 查询历史数据 |
| POST | `/api/sensor/batch` | 批量接收数据 |

### 聊天接口

| 方法 | 端点 | 说明 |
|------|------|------|
| POST | `/api/chat/send` | HTTP发送消息 |
| WebSocket | `/api/chat/ws` | WebSocket连接 |
| GET | `/api/chat/history/{sessionId}` | 查询历史 |
| DELETE | `/api/chat/history/{sessionId}` | 清空历史 |

### 图像识别

| 方法 | 端点 | 说明 |
|------|------|------|
| POST | `/flask/predict` | 识别图像并生成方案 |
| GET | `/flask/file_names` | 获取权重文件列表 |

---

## 八、性能监控

### 后端监控

```bash
# 查看日志
tail -f yolo_cropDisease_detection_springboot/logs/spring-boot.log

# 监控内存使用
jps -l
jstat -gc [PID] 1000
```

### 数据库监控

```sql
-- 查询传感器数据量
SELECT COUNT(*) FROM tb_sensor_data;

-- 查询最新10条数据
SELECT * FROM tb_sensor_data ORDER BY timestamp DESC LIMIT 10;

-- 查询数据统计
SELECT 
  MIN(timestamp) AS 最早时间,
  MAX(timestamp) AS 最新时间,
  AVG(temperature) AS 平均温度,
  AVG(humidity) AS 平均湿度
FROM tb_sensor_data;
```

---

## 九、下一步

### 功能扩展

1. **真实传感器接入**：
   - 配置物联网设备推送地址为 `http://your-server:9999/api/sensor/data`
   - 设置数据推送频率（建议5-10分钟一次）

2. **施药提醒**：
   - 根据时间窗口推送施药提醒
   - 集成企业微信/钉钉通知

3. **方案优化**：
   - 收集用户反馈
   - 优化施药时间窗口算法

### 性能优化

1. **Redis缓存**：
   - 缓存聊天会话上下文
   - 缓存气象数据

2. **数据库优化**：
   - 定期清理历史数据（保留90天）
   - 添加更多索引

3. **前端优化**：
   - 图表数据懒加载
   - 聊天消息分页

---

**祝您使用愉快！如有问题，请查看《第二阶段实施总结.md》获取详细信息。**


