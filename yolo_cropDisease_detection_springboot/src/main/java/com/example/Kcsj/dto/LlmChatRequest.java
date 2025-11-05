package com.example.Kcsj.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * LLM 聊天请求 DTO（发送给讯飞星火的请求体）
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LlmChatRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 会话ID（用于多轮对话）
     */
    private String sessionId;
    
    /**
     * 用户消息内容
     */
    private String message;
    
    /**
     * 历史消息列表（上下文）
     */
    private List<LlmChatMessage> history;
    
    /**
     * 是否使用流式输出
     */
    private Boolean stream;
    
    /**
     * 温度参数（控制随机性，0.0-1.0）
     */
    private Double temperature;
    
    /**
     * 最大生成长度
     */
    private Integer maxTokens;
}


