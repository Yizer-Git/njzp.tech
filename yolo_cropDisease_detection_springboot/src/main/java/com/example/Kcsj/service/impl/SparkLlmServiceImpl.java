package com.example.Kcsj.service.impl;

import cn.hutool.core.util.StrUtil;
import com.example.Kcsj.config.LlmConfig;
import com.example.Kcsj.dto.LlmChatMessage;
import com.example.Kcsj.dto.LlmChatRequest;
import com.example.Kcsj.dto.LlmChatResponse;
import com.example.Kcsj.service.SparkLlmService;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 讯飞星火大模型服务实现类
 * 支持真实API调用和WebSocket流式响应
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SparkLlmServiceImpl implements SparkLlmService {
    
    private final LlmConfig llmConfig;
    private final Gson gson = new Gson();
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();
    
    @Override
    public LlmChatResponse sendMessage(LlmChatRequest request) {
        log.info("收到LLM聊天请求，会话ID: {}, 消息: {}", request.getSessionId(), request.getMessage());
        
        // 检查配置是否完整
        if (!llmConfig.isConfigured()) {
            log.warn("讯飞星火API未配置，返回模拟响应");
            return buildMockResponse(request);
        }
        
        try {
            // 调用真实的讯飞星火API
            String realResponse = callSparkWebSocket(request);
            return LlmChatResponse.builder()
                    .sessionId(request.getSessionId())
                    .content(realResponse)
                    .isEnd(true)
                    .status(0)
                    .timestamp(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            log.error("调用讯飞星火API失败", e);
            return LlmChatResponse.builder()
                    .sessionId(request.getSessionId())
                    .content("抱歉，AI服务暂时不可用，请稍后重试。")
                    .isEnd(true)
                    .status(-1)
                    .errorMessage(e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }
    
    @Override
    public void sendMessageStream(LlmChatRequest request, StreamCallback callback) {
        log.info("收到 LLM 流式请求，会话ID: {}, 消息: {}", request.getSessionId(), request.getMessage());
        try {
            if (!llmConfig.isConfigured()) {
                log.warn("讯飞星火 API 未配置，改用模拟流式响应");
                simulateStreamResponse(request, callback);
                return;
            }

            callSparkWebSocketStream(request, callback);
        } catch (Exception e) {
            log.error("处理流式响应失败", e);
            callback.onError(e);
        }
    }
    
    /**
     * 构建模拟响应
     */
    private LlmChatResponse buildMockResponse(LlmChatRequest request) {
        String mockContent = generateMockContent(request.getMessage());
        
        return LlmChatResponse.builder()
                .sessionId(request.getSessionId())
                .content(mockContent)
                .isEnd(true)
                .status(0)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * 模拟流式响应
     */
    private void simulateStreamResponse(LlmChatRequest request, StreamCallback callback) {
        String fullResponse = generateMockContent(request.getMessage());
        simulateStreamResponse(fullResponse, callback);
    }
    
    /**
     * 模拟流式响应（重载方法）
     */
    private void simulateStreamResponse(String fullResponse, StreamCallback callback) {
        // 模拟逐字输出
        String[] words = fullResponse.split("");
        for (String word : words) {
            callback.onMessage(word);
            try {
                Thread.sleep(50);  // 模拟打字延迟
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        callback.onComplete();
    }
    
    /**
     * 生成模拟内容（根据用户问题生成合理的回复）
     */
    
        private String generateMockContent(String userMessage) {
        if (StrUtil.isBlank(userMessage)) {
            return "你好，我是智能农事助手，如需巡田巡查或病害诊断，请随时提问。";
        }

        String lowerMessage = userMessage.toLowerCase(Locale.ROOT);

        if (lowerMessage.contains("病") || lowerMessage.contains("害")) {
            return String.join(
                    "\n",
                    "识别到您在咨询常见病害，可参考：",
                    "1. 使用图像识别确认病害类型；",
                    "2. 查看方案中心的风险等级与推荐药剂；",
                    "3. 若症状异常，请同步上传记录并联系线下农技人员。"
            );
        } else if (lowerMessage.contains("施药") || lowerMessage.contains("打药")) {
            return String.join(
                    "\n",
                    "施药前请注意：",
                    "- 选择风速小于 5m/s、预报 4 小时内无降雨的时间窗口；",
                    "- 按照系统推荐的兑水比例与防治药剂配置药液；",
                    "- 记录任何不适与药害，便于跟踪追溯。"
            );
        } else if (lowerMessage.contains("气象") || lowerMessage.contains("天气")) {
            return String.join(
                    "\n",
                    "平台已对接气象监测站，提供未来 48 小时温湿度、降雨量及风险预警数据，",
                    "可通过“推荐施药时间窗”功能联动作业计划。"
            );
        } else if (lowerMessage.contains("传感") || lowerMessage.contains("设备")) {
            return String.join(
                    "\n",
                    "传感器中心可查看温度、湿度、土壤墒情等关键指标，支持阈值预警和与识别记录联动的状态分析。"
            );
        }

        String template = String.join(
                "\n",
                "您好，我是智能农事助手，目前支持：",
                "- 图像与视频病害识别",
                "- 智能防治方案生成",
                "- 施药指导与环境监测联动",
                "- 设备远程控制与日志追溯",
                "",
                "您刚才的提问是：%s",
                "欢迎继续咨询病害诊断、施药、气象与作业相关的问题。"
        );
        return String.format(template, userMessage);
    }// ========== 真实API调用方法 ==========
    
    /**
     * 调用真实的讯飞星火API
     */
        private String callSparkWebSocket(LlmChatRequest request) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        StringBuilder buffer = new StringBuilder();
        AtomicReference<Exception> errorRef = new AtomicReference<>();
        AtomicBoolean completed = new AtomicBoolean(false);

        Request requestConfig = buildWebSocketRequest();
        WebSocket webSocket = httpClient.newWebSocket(requestConfig, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                webSocket.send(buildWebSocketPayload(request, false));
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                boolean finish = handleSparkMessage(text, buffer, null);
                if (finish && completed.compareAndSet(false, true)) {
                    latch.countDown();
                    webSocket.close(1000, "completed");
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                errorRef.compareAndSet(null, t instanceof Exception ? (Exception) t : new IOException(t));
                if (completed.compareAndSet(false, true)) {
                    latch.countDown();
                }
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                if (completed.compareAndSet(false, true)) {
                    latch.countDown();
                }
            }
        });

        if (!latch.await(45, TimeUnit.SECONDS)) {
            webSocket.cancel();
            throw new IOException("Spark WebSocket timeout");
        }

        if (errorRef.get() != null) {
            throw errorRef.get();
        }

        return buffer.length() > 0 ? buffer.toString() : "";
    }

    private void callSparkWebSocketStream(LlmChatRequest request, StreamCallback callback) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> errorRef = new AtomicReference<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        StringBuilder buffer = new StringBuilder();

        Request requestConfig = buildWebSocketRequest();
        WebSocket webSocket = httpClient.newWebSocket(requestConfig, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                webSocket.send(buildWebSocketPayload(request, true));
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                boolean finish = handleSparkMessage(text, buffer, callback);
                if (finish && completed.compareAndSet(false, true)) {
                    callback.onComplete();
                    latch.countDown();
                    webSocket.close(1000, "ok");
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                errorRef.compareAndSet(null, t instanceof Exception ? (Exception) t : new IOException(t));
                if (completed.compareAndSet(false, true)) {
                    callback.onError(errorRef.get());
                    latch.countDown();
                }
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                if (completed.compareAndSet(false, true)) {
                    callback.onComplete();
                    latch.countDown();
                }
            }
        });

        if (!latch.await(45, TimeUnit.SECONDS)) {
            webSocket.cancel();
            throw new IOException("Spark WebSocket timeout");
        }

        if (errorRef.get() != null) {
            throw errorRef.get();
        }
    }

    private Request buildWebSocketRequest() throws Exception {
        String authUrl = buildAuthenticatedWsUrl();
        return new Request.Builder()
                .url(authUrl)
                .build();
    }

    private String buildWebSocketPayload(LlmChatRequest request, boolean stream) {
        JsonObject payload = new JsonObject();

        JsonObject header = new JsonObject();
        header.addProperty("app_id", llmConfig.getAppId());
        if (StrUtil.isNotBlank(request.getSessionId())) {
            header.addProperty("uid", request.getSessionId());
        }

        JsonObject parameter = new JsonObject();
        JsonObject chatParam = new JsonObject();
        chatParam.addProperty("domain", StrUtil.isNotBlank(llmConfig.getModelVersion()) ? llmConfig.getModelVersion() : "general");
        chatParam.addProperty("temperature", request.getTemperature() != null ? request.getTemperature() : 0.7);
        chatParam.addProperty("max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : 2048);
        chatParam.addProperty("stream", stream);
        chatParam.addProperty("auditing", "default");
        parameter.add("chat", chatParam);

        JsonObject payloadRoot = new JsonObject();
        JsonObject messageObject = new JsonObject();
        JsonArray textArray = new JsonArray();

        List<LlmChatMessage> history = request.getHistory() != null ? request.getHistory() : Collections.emptyList();
        for (LlmChatMessage message : history) {
            textArray.add(buildMessageNode(message.getRole(), message.getContent()));
        }
        textArray.add(buildMessageNode("user", request.getMessage()));

        messageObject.add("text", textArray);
        payloadRoot.add("message", messageObject);

        payload.add("header", header);
        payload.add("parameter", parameter);
        payload.add("payload", payloadRoot);
        return gson.toJson(payload);
    }

    private JsonObject buildMessageNode(String role, String content) {
        JsonObject node = new JsonObject();
        node.addProperty("role", role);
        JsonArray contentArr = new JsonArray();
        JsonObject textObj = new JsonObject();
        textObj.addProperty("text", StrUtil.blankToDefault(content, ""));
        contentArr.add(textObj);
        node.add("content", contentArr);
        return node;
    }

    private boolean handleSparkMessage(String text, StringBuilder buffer, StreamCallback callback) {
        try {
            JsonObject root = gson.fromJson(text, JsonObject.class);
            int status = 0;
            if (root.has("header")) {
                JsonObject header = root.getAsJsonObject("header");
                if (header.has("status")) {
                    status = header.get("status").getAsInt();
                }
            }

            String delta = extractContent(root);
            if (StrUtil.isNotBlank(delta)) {
                buffer.append(delta);
                if (callback != null) {
                    callback.onMessage(delta);
                }
            }

            return status == 2;
        } catch (Exception ex) {
            log.debug("解析星火返回的数据失败: {}", text, ex);
            return false;
        }
    }

    private String extractContent(JsonObject root) {
        if (root == null) {
            return null;
        }
        if (root.has("payload")) {
            JsonObject payload = root.getAsJsonObject("payload");
            if (payload.has("choices")) {
                JsonObject choices = payload.getAsJsonObject("choices");
                if (choices.has("text")) {
                    StringBuilder builder = new StringBuilder();
                    choices.getAsJsonArray("text").forEach(element -> {
                        JsonObject obj = element.getAsJsonObject();
                        if (obj.has("content")) {
                            obj.getAsJsonArray("content").forEach(contentItem -> {
                                JsonObject contentObj = contentItem.getAsJsonObject();
                                if (contentObj.has("text")) {
                                    builder.append(contentObj.get("text").getAsString());
                                }
                            });
                        }
                    });
                    if (builder.length() > 0) {
                        return builder.toString();
                    }
                }
            }
        }
        if (root.has("choices")) {
            // 兼容 HTTP 接口的返回格式
            JsonObject choice = root.getAsJsonArray("choices").get(0).getAsJsonObject();
            if (choice.has("message")) {
                JsonObject message = choice.getAsJsonObject("message");
                if (message.has("content")) {
                    return message.get("content").getAsString();
                }
            }
        }
        return null;
    }

    private String buildAuthenticatedWsUrl() throws Exception {
        String wssUrl = llmConfig.getWssUrl();
        if (StrUtil.isBlank(wssUrl)) {
            throw new IllegalStateException("未配置必要的 WebSocket 接口 URL");
        }
        URI uri = URI.create(wssUrl);
        String host = uri.getHost();
        String path = uri.getRawPath();

        String date = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
                .format(new Date());
        String authorization = generateSignature(host, path, date);

        String encodedAuth = URLEncoder.encode(authorization, StandardCharsets.UTF_8.name());
        String encodedDate = URLEncoder.encode(date, StandardCharsets.UTF_8.name());

        return String.format("%s?authorization=%s&date=%s&host=%s", wssUrl, encodedAuth, encodedDate, host);
    }

    private String generateSignature(String host, String path, String date) throws Exception {
        String stringToSign = String.format("host: %s\ndate: %s\nGET %s HTTP/1.1", host, date, path);
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(llmConfig.getApiSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] signatureBytes = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getEncoder().encodeToString(signatureBytes);
        return String.format("api_key=\"%s\", algorithm=\"hmac-sha256\", headers=\"host date request-line\", signature=\"%s\"",
                llmConfig.getApiKey(), signature);
    }
}

