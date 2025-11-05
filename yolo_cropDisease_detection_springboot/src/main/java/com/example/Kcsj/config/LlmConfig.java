package com.example.Kcsj.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 讯飞星火大模型配置类
 */
@Configuration
@ConfigurationProperties(prefix = "llm.spark")
@Data
public class LlmConfig {
    
    /**
     * 讯飞开放平台应用ID
     */
    private String appId;
    
    /**
     * API Key
     */
    private String apiKey;
    
    /**
     * API Secret（用于签名鉴权）
     */
    private String apiSecret;
    
    /**
     * WebSocket 请求地址
     */
    private String wssUrl;
    
    /**
     * 模型版本（如：v3.5）
     */
    private String modelVersion;
    
    /**
     * 检查配置是否完整
     */
    public boolean isConfigured() {
        return appId != null && !appId.equals("YOUR_APP_ID") &&
               apiKey != null && !apiKey.equals("YOUR_API_KEY") &&
               apiSecret != null && !apiSecret.equals("YOUR_API_SECRET");
    }
}


