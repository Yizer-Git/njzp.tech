package com.example.Kcsj.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * LLM 聊天消息 DTO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LlmChatMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 消息角色：user（用户）、assistant（助手）、system（系统）
     */
    private String role;
    
    /**
     * 消息内容
     */
    private String content;
}


