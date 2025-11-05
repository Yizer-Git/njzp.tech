package com.example.Kcsj.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * LLM 聊天响应 DTO（接收讯飞星火的响应）
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LlmChatResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * AI 回复内容
     */
    private String content;
    
    /**
     * 是否是流式输出的最后一条消息
     */
    private Boolean isEnd;
    
    /**
     * 状态码（0-成功，其他-失败）
     */
    private Integer status;
    
    /**
     * 错误消息
     */
    private String errorMessage;
    
    /**
     * 响应时间
     */
    private LocalDateTime timestamp;
}


