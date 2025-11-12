# “农疾智判”项目硬件集成 TODO 文档



本文档详细说明了将“AI 农作物智慧监测终端”（基于《硬件原理设计.docx》）集成到现有 `yolo_cropDisease_detection_springboot` 和 `yolo_cropDisease_detection_vue` 项目所需的全部任务。

**核心通信协议：** [MQTT](https://mqtt.org/) (推荐，实时性强) **备选通信协议：** HTTP (用于简单上报)

------



## 第 1 部分：后端 (yolo_cropDisease_detection_springboot)



**目标：** 通过 MQTT 接收硬件数据，并通过 API 允许前端下发控制指令。



### 任务 1.1：(数据模型) 扩展传感器实体



- **文件：** `src/main/java/com/example/Kcsj/entity/SensorData.java`
- **动作：** 根据《硬件原理设计.docx》 中的传感器，添加新字段。

Java

```
package com.example.Kcsj.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
// ... (其他导入)
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sensor_data")
public class SensorData implements Serializable {

    private static final long serialVersionUID = 1L;

    // ... (保留 id 和 createTime)

    /**
     * 终端设备唯一ID
     */
    @TableField("device_id") // 确保数据库有此字段
    private String deviceId;

    /**
     * 土壤湿度 (来自 YL69/YL38)
     */
    @TableField("soil_humidity")
    private Double soilHumidity;

    /**
     * 空气/冠层温度 (来自 DS18B20)
     */
    @TableField("air_temperature")
    private Double airTemperature;

    /**
     * 光照强度 (来自 5528光敏电阻)
     */
    @TableField("light_intensity")
    private Double lightIntensity;

    /**
     * 水位状态 (来自 水位传感器YL69)
     * (0=正常, 1=缺水, 2=水满 - 根据您的传感器定义)
     */
    @TableField("water_level")
    private Integer waterLevel;
    
    // ... (您现有的其他字段)
}
```



### 任务 1.2：(MQTT) 添加依赖



- **文件：** `pom.xml`
- **动作：** 在 `<dependencies>` 块中添加 Spring Integration MQTT 依赖。

XML

```
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-integration</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.integration</groupId>
    <artifactId>spring-integration-mqtt</artifactId>
</dependency>
<dependency>
    <groupId>org.eclipse.paho</groupId>
    <artifactId>org.eclipse.paho.mqttv5.client</artifactId>
    <version>1.2.5</version> 
</dependency>
```



### 任务 1.3：(MQTT) 添加配置



- **文件：** `src/main/resources/application.properties`
- **动作：** 添加 MQTT Broker（如 EMQX, Mosquitto）的连接信息。

Properties

```
# --- MQTT Broker Configuration ---
# 替换为您的 MQTT Broker 地址
mqtt.broker.url=tcp://[your-mqtt-broker-ip]:1883
mqtt.broker.username=[your-mqtt-username]
mqtt.broker.password=[your-mqtt-password]
mqtt.client.id=springboot_backend_service

# --- MQTT Topics ---
# 订阅硬件上报数据的主题 (使用通配符 '+' 匹配所有 deviceId)
mqtt.topic.sensor-upload=device/sensor/upload/+
# 定义硬件控制指令的基础主题
mqtt.topic.control-prefix=device/control/
```



### 任务 1.4：(MQTT) 创建 MQTT 配置和监听器



- **文件 (新建)：** `src/main/java/com/example/Kcsj/config/MqttConfig.java`
- **动作：** 配置 MQTT 客户端工厂、消息通道、入站监听器（接收数据）和出站网关（发送指令）。

Java

```
package com.example.Kcsj.config;

import com.example.Kcsj.entity.SensorData;
import com.example.Kcsj.service.SensorService; // 假设您有
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import java.time.LocalDateTime;

@Configuration
public class MqttConfig {

    // 注入 application.properties 中的配置
    @Value("${mqtt.broker.url}") private String brokerUrl;
    @Value("${mqtt.broker.username}") private String username;
    @Value("${mqtt.broker.password}") private String password;
    @Value("${mqtt.client.id}") private String clientId;
    @Value("${mqtt.topic.sensor-upload}") private String sensorUploadTopic;

    @Autowired
    private SensorService sensorService; // 注入Service用于存储数据
    
    @Autowired
    private ObjectMapper objectMapper; // 自动注入Spring的ObjectMapper

    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setServerURIs(new String[]{brokerUrl});
        options.setUserName(username);
        options.setPassword(password.getBytes());
        options.setCleanStart(true);
        options.setAutomaticReconnect(true); // 开启自动重连
        factory.setConnectionOptions(options);
        return factory;
    }

    // 1. ============ 入站 (Hardware -> Backend) ============
    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MqttPahoMessageDrivenChannelAdapter inbound() {
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
                clientId + "_inbound", mqttClientFactory(), sensorUploadTopic);
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1); // 服务质量 1: 至少一次
        adapter.setOutputChannel(mqttInputChannel());
        return adapter;
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public MessageHandler mqttInputHandler() {
        return message -> {
            String payload = message.getPayload().toString();
            System.out.println("Received MQTT Message: " + payload);
            try {
                // 解析JSON
                SensorData sensorData = objectMapper.readValue(payload, SensorData.class);
                sensorData.setCreateTime(LocalDateTime.now()); // 设置服务器接收时间
                
                // 从Topic中解析DeviceId, e.g., device/sensor/upload/ESP32_001
                String topic = message.getHeaders().get("mqtt_receivedTopic").toString();
                String deviceId = topic.substring(topic.lastIndexOf('/') + 1);
                sensorData.setDeviceId(deviceId);

                sensorService.save(sensorData); // 存入数据库
            } catch (Exception e) {
                System.err.println("Failed to parse or save MQTT message: " + e.getMessage());
            }
        };
    }
    
    // 2. ============ 出站 (Backend -> Hardware) ============
    @Bean
    public MessageChannel mqttOutputChannel() {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttOutputChannel")
    public MessageHandler mqttOutputHandler() {
        MqttPahoMessageHandler handler = new MqttPahoMessageHandler(clientId + "_outbound", mqttClientFactory());
        handler.setAsync(true); // 异步发送
        handler.setDefaultQos(1);
        return handler;
    }

    // 3. ============ 消息网关 (供Controller调用) ============
    @MessagingGateway(defaultRequestChannel = "mqttOutputChannel")
    public interface MqttGateway {
        /**
         * 发送消息
         * @param topic 主题
         * @param payload 消息体 (String/JSON)
         */
        void sendToMqtt(String topic, String payload);
    }
}
```



### 任务 1.5：(MQTT) 创建硬件控制 API



- **文件：** `src/main/java/com/example/Kcsj/controller/DeviceController.java`
- **动作：** 注入 `MqttGateway`，创建一个被前端调用的 API，用于下发指令。

Java

```
package com.example.Kcsj.controller;

import com.example.Kcsj.common.Result;
import com.example.Kcsj.config.MqttConfig.MqttGateway; // 导入内部接口
import com.example.Kcsj.dto.DeviceControlRequest; // 确保这个DTO存在
import com.fasterxml.jackson.databind.ObjectMapper; // 导入 ObjectMapper
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/device")
public class DeviceController {

    @Autowired
    private MqttGateway mqttGateway;
    
    @Value("${mqtt.topic.control-prefix}")
    private String controlTopicPrefix;
    
    @Autowired
    private ObjectMapper objectMapper; // 用于构建JSON

    /**
     * [新增] 硬件控制接口 (供VUE前端调用)
     * @param request 包含 deviceId, action, value
     * @return Result
     */
    @PostMapping("/control")
    public Result controlDevice(@RequestBody DeviceControlRequest request) {
        try {
            // 1. 确定目标主题, e.g., "device/control/ESP32_001"
            String topic = controlTopicPrefix + request.getDeviceId();
            
            // 2. 构建JSON指令 (硬件需要能解析这个)
            // DeviceControlRequest 应该包含 { deviceId, action, value }
            String payload = objectMapper.writeValueAsString(request); 

            // 3. 发送MQTT消息
            mqttGateway.sendToMqtt(topic, payload);
            
            // (可选) 记录操作日志到数据库 (您已有 DeviceControlLogMapper)
            
            return Result.success("Command sent to " + request.getDeviceId());
        } catch (Exception e) {
            return Result.error("500", "Failed to send command: " + e.getMessage());
        }
    }
    
    // ... (保留您现有的其他方法)
}
```



### 任务 1.6：(备选方案 - HTTP) 创建硬件上报 API



- **文件：** `src/main/java/com/example/Kcsj/controller/SensorController.java`
- **动作：** 添加一个 `@PostMapping` 端点供硬件直接调用。

Java

```
// ... (在 SensorController.java 中)
import com.example.Kcsj.entity.SensorData;
import java.time.LocalDateTime;

    /**
     * [备选] 硬件上报数据接口 (HTTP)
     * 供 ESP32 终端调用
     */
    @PostMapping("/upload")
    public Result uploadSensorDataHttp(@RequestBody SensorData sensorData) {
        try {
            sensorData.setCreateTime(LocalDateTime.now());
            // 必须确保 sensorData 中包含 deviceId
            if (sensorData.getDeviceId() == null || sensorData.getDeviceId().isEmpty()) {
                 return Result.error("400", "deviceId is required");
            }
            sensorService.save(sensorData);
            return Result.success("Data received");
        } catch (Exception e) {
            return Result.error("500", "Server error");
        }
    }
// ...
```

------



## 第 2 部分：前端 (yolo_cropDisease_detection_vue)



**目标：** 新增 "设备控制" 页面，并在 "传感器" 页面展示新数据。



### 任务 2.1：(数据展示) 更新传感器数据页面



- **文件：** `src/views/sensor/index.vue`
- **动作：** 确保 API 请求能获取新字段，并在 `<el-table>` 中添加新列。

HTML

```
<el-table-column
  prop="deviceId"
  label="设备ID"
  align="center">
</el-table-column>
<el-table-column
  prop="airTemperature"
  label="空气温度 (°C)"
  align="center">
</el-table-column>
<el-table-column
  prop="soilHumidity"
  label="土壤湿度 (%)"
  align="center">
</el-table-column>
<el-table-column
  prop="lightIntensity"
  label="光照强度 (Lux)"
  align="center">
</el-table-column>
```



### 任务 2.2：(设备控制) 创建设备控制 API



- **文件：** `src/api/device.ts`
- **动作：** 添加一个函数，用于调用后端的 `/device/control` 接口。

TypeScript

```
import request from '@/utils/request';

// (您可能已有其他函数)

/**
 * 定义控制请求的类型
 */
interface ControlRequest {
  deviceId: string;
  action: string; // e.g., "RELAY_WATER", "RELAY_FAN", "BUZZER"
  value: any;     // e.g., true, false, 1000 (蜂鸣器响1秒)
}

/**
 * [新增] 发送设备控制指令
 * @param data 控制请求
 */
export function controlDevice(data: ControlRequest) {
  return request({
    url: '/device/control', // 对应 Spring Boot 的 @PostMapping("/device/control")
    method: 'post',
    data: data
  });
}
```



### 任务 2.3：(设备控制) 创建设备控制页面 (View)



- **文件 (新建)：** `src/views/deviceControl/index.vue`
- **动作：** 创建一个新页面，包含用于控制继电器和蜂鸣器的开关或按钮。

代码段

```
<template>
  <div class="app-container">
    <el-card>
      <div slot="header" class="clearfix">
        <span>硬件远程控制台</span>
      </div>
      
      <el-select v-model="selectedDeviceId" placeholder="请选择设备" style="width: 100%; margin-bottom: 20px;">
        <el-option label="农场一号机 (ESP32_001)" value="ESP32_001"></el-option>
        <el-option label="农场二号机 (ESP32_002)" value="ESP32_002"></el-option>
      </el-select>
      <el-divider></el-divider>

      <el-row :gutter="20">
        <el-col :span="8">
          <el-card>
            <div slot="header">水泵 (继电器1)</div>
            <div style="text-align: center;">
              <el-switch
                v-model="relayWaterState"
                @change="handleRelayChange('RELAY_WATER', $event)"
                active-text="开启"
                inactive-text="关闭"
                :disabled="!selectedDeviceId"
              ></el-switch>
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card>
            <div slot="header">风扇 (继电器2)</div>
            <div style="text-align: center;">
              <el-switch
                v-model="relayFanState"
                @change="handleRelayChange('RELAY_FAN', $event)"
                active-text="开启"
                inactive-text="关闭"
                :disabled="!selectedDeviceId"
              ></el-switch>
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card>
            <div slot="header">警报器 (蜂鸣器)</div>
            <div style="text-align: center;">
              <el-button 
                @click="handleAction('BUZZER', true)" 
                type="danger" 
                :disabled="!selectedDeviceId">
                触发警报
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { controlDevice } from '@/api/device'; // 导入API

const selectedDeviceId = ref('ESP32_001'); // 默认设备ID
const relayWaterState = ref(false);
const relayFanState = ref(false);

// 处理开关（继电器）
const handleRelayChange = async (action: string, value: boolean) => {
  handleAction(action, value);
};

// 通用处理函数
const handleAction = async (action: string, value: any) => {
  if (!selectedDeviceId.value) {
    ElMessage.warning('请先选择一个设备');
    // 重置状态
    if (action === 'RELAY_WATER') relayWaterState.value = !value;
    if (action === 'RELAY_FAN') relayFanState.value = !value;
    return;
  }

  try {
    const res = await controlDevice({
      deviceId: selectedDeviceId.value,
      action: action,
      value: value
    });
    
    // 假设您的 Result.success() 返回 code '200'
    if (res.code === '200') { 
      ElMessage.success(`指令 [${action}: ${value}] 已发送`);
    } else {
      ElMessage.error('指令发送失败: ' + res.message);
      // 失败时重置开关状态
      if (action === 'RELAY_WATER') relayWaterState.value = !value;
      if (action === 'RELAY_FAN') relayFanState.value = !value;
    }
  } catch (error) {
    ElMessage.error('请求异常');
    if (action === 'RELAY_WATER') relayWaterState.value = !value;
    if (action === 'RELAY_FAN') relayFanState.value = !value;
  }
};
</script>
```



### 任务 2.4：(设备控制) 添加路由



- **文件：** `src/router/backEnd.ts` (或 `index.ts`)
- **动作：** 将新创建的 `deviceControl` 页面添加为路由，并加入到侧边栏菜单。
- **提示：** 您需要将此路由添加到 `layout` 组件的 `children` 数组中，以便显示在主布局内。

TypeScript

```
// ... (在 routes 数组中，通常在 layout 的 children)
{
    path: '/device-control',
    name: 'deviceControl',
    component: () => import('@/views/deviceControl/index.vue'),
    meta: {
        title: '设备控制', // 菜单标题
        icon: 'ele-Cpu', // 菜单图标 (Element Plus aicon)
        keepAlive: false
    }
}
// ...
```

------



## 第 3 部分：硬件 (ESP32) 伪代码“契约”



**目标：** 提供给硬件开发者的代码指南，确保其与后端的 MQTT 接口兼容。

- **文件：** `ESP32_Firmware.ino` (示例文件名)
- **所需库：** `WiFi.h`, `PubSubClient.h`, `ArduinoJson.h` (v6+), `OneWire.h`, `DallasTemperature.h`

C++

```
#include <WiFi.h>
#include <PubSubClient.h> // MQTT
#include <ArduinoJson.h>  // JSON
#include <OneWire.h>      // DS18B20
#include <DallasTemperature.h> // DS18B20

// --- 1. 配置 (必须修改) ---
const char* WIFI_SSID = "[your_wifi_ssid]";
const char* WIFI_PASS = "[your_wifi_password]";
const char* MQTT_BROKER = "[your-mqtt-broker-ip]";
const int   MQTT_PORT = 1883;
const char* MQTT_USER = "[your-mqtt-username]";
const char* MQTT_PASS = "[your-mqtt-password]";

// 唯一的设备ID, 必须和后端匹配
const char* DEVICE_ID = "ESP32_001"; 

// 主题 (必须和 Spring Boot 配置匹配)
char SENSOR_TOPIC[100]; // "device/sensor/upload/ESP32_001"
char CONTROL_TOPIC[100]; // "device/control/ESP32_001"

// --- 2. 硬件引脚定义 (来自原理图) ---
#define SOIL_PIN 36      // YL69 ADC (A区)
#define DS18B20_PIN 4    // DS18B20 (C区)
#define RELAY_WATER_PIN 26 // 继电器1 (B区, 假设)
#define RELAY_FAN_PIN 27   // 继电器2 (B区, 假设)
#define BUZZER_PIN 14      // 蜂鸣器 (A区, 假设)

// --- 3. 库实例 ---
WiFiClient espClient;
PubSubClient client(espClient);
OneWire oneWire(DS18B20_PIN);
DallasTemperature tempSensors(&oneWire);

// --- 4. MQTT 回调 (接收指令) ---
void mqttCallback(char* topic, byte* payload, unsigned int length) {
    Serial.print("Message arrived [");
    Serial.print(topic);
    Serial.print("] ");

    // 1. 解析JSON指令
    StaticJsonDocument<256> doc;
    deserializeJson(doc, payload, length);
    
    // 2. 获取指令 (匹配前端的 ControlRequest)
    const char* action = doc["action"]; // e.g., "RELAY_WATER"
    bool value = doc["value"];          // e.g., true

    Serial.print("Action: ");
    Serial.print(action);
    Serial.print(", Value: ");
    Serial.println(value);

    // 3. 执行指令
    if (strcmp(action, "RELAY_WATER") == 0) {
        digitalWrite(RELAY_WATER_PIN, value ? HIGH : LOW);
        Serial.println(value ? "Water Relay ON" : "Water Relay OFF");
    } 
    else if (strcmp(action, "RELAY_FAN") == 0) {
        digitalWrite(RELAY_FAN_PIN, value ? HIGH : LOW);
        Serial.println(value ? "Fan Relay ON" : "Fan Relay OFF");
    }
    else if (strcmp(action, "BUZZER") == 0) {
        digitalWrite(BUZZER_PIN, value ? HIGH : LOW);
        // (蜂鸣器可能需要脉冲, 这里简化为开关)
        if(value) {
            delay(1000); // 响1秒
            digitalWrite(BUZZER_PIN, LOW);
        }
    }
}

// --- 5. (MQTT) 重连 ---
void mqttReconnect() {
    while (!client.connected()) {
        Serial.print("Attempting MQTT connection...");
        if (client.connect(DEVICE_ID, MQTT_USER, MQTT_PASS)) {
            Serial.println("connected");
            // 订阅控制主题
            client.subscribe(CONTROL_TOPIC);
            Serial.print("Subscribed to: ");
            Serial.println(CONTROL_TOPIC);
        } else {
            Serial.print("failed, rc=");
            Serial.print(client.state());
            Serial.println(" try again in 5 seconds");
            delay(5000);
        }
    }
}

// --- 6. (数据) 读取传感器 ---
float readAirTemperature() {
    tempSensors.requestTemperatures();
    return tempSensors.getTempCByIndex(0);
}

float readSoilHumidity() {
    int adcVal = analogRead(SOIL_PIN);
    // (您需要根据YL69的特性进行校准)
    return (1.0 - (adcVal / 4095.0)) * 100.0; 
}

// --- 7. (数据) 上报传感器数据 ---
void publishSensorData() {
    // 1. 读取数据
    float temp = readAirTemperature();
    float humidity = readSoilHumidity();
    // (读取光照和水位...)
    
    Serial.print("Sending data: T=");
    Serial.print(temp);
    Serial.print(", H=");
    Serial.println(humidity);

    // 2. 构建 JSON (匹配 SensorData.java)
    StaticJsonDocument<256> doc;
    // doc["deviceId"] = DEVICE_ID; // Spring Boot会从Topic解析, Payload中可以不带
    doc["airTemperature"] = temp;
    doc["soilHumidity"] = humidity;
    doc["lightIntensity"] = 0; // (待实现)
    doc["waterLevel"] = 0;     // (待实现)

    // 3. 序列化并发布
    char buffer[256];
    serializeJson(doc, buffer);
    client.publish(SENSOR_TOPIC, buffer);
}

// --- 8. SETUP ---
void setup() {
    Serial.begin(115200);
    // 初始化引脚
    pinMode(RELAY_WATER_PIN, OUTPUT);
    pinMode(RELAY_FAN_PIN, OUTPUT);
    pinMode(BUZZER_PIN, OUTPUT);
    pinMode(SOIL_PIN, INPUT); // ADC
    
    // 启动传感器
    tempSensors.begin();
    
    // 连接WiFi
    Serial.print("Connecting to ");
    Serial.println(WIFI_SSID);
    WiFi.begin(WIFI_SSID, WIFI_PASS);
    while (WiFi.status() != WL_CONNECTED) {
        delay(500);
        Serial.print(".");
    }
    Serial.println("WiFi connected.");

    // 构建主题
    sprintf(SENSOR_TOPIC, "device/sensor/upload/%s", DEVICE_ID);
    sprintf(CONTROL_TOPIC, "device/control/%s", DEVICE_ID);

    // 连接MQTT
    client.setServer(MQTT_BROKER, MQTT_PORT);
    client.setCallback(mqttCallback); // 设置指令回调
}

// --- 9. LOOP ---
unsigned long lastMsg = 0;
long msgInterval = 30000; // 30秒上报一次

void loop() {
    if (!client.connected()) {
        mqttReconnect(); // 检查MQTT连接
    }
    client.loop(); // 必须调用, 处理MQTT消息 (包括接收指令)

    // 定时上报数据
    unsigned long now = millis();
    if (now - lastMsg > msgInterval) {
        lastMsg = now;
        publishSensorData();
    }
}
```



1