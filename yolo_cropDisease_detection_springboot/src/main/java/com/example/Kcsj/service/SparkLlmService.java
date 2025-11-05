package com.example.Kcsj.service;

import com.example.Kcsj.dto.LlmChatRequest;
import com.example.Kcsj.dto.LlmChatResponse;

/**
 * 讯飞星火大模型服务接口
 */
public interface SparkLlmService {
    
    /**
     * 发送聊天消息（同步方式）
     * @param request 聊天请求
     * @return 聊天响应
     */
    LlmChatResponse sendMessage(LlmChatRequest request);
    
    /**
     * 发送聊天消息（异步流式方式）
     * 注意：此方法预留，暂不实现实际的 WebSocket 流式响应
     * @param request 聊天请求
     * @param callback 流式响应回调
     */
    void sendMessageStream(LlmChatRequest request, StreamCallback callback);
    
    /**
     * 流式响应回调接口
     */
    interface StreamCallback {
        /**
         * 接收到新的内容片段
         */
        void onMessage(String content);
        
        /**
         * 流式响应结束
         */
        void onComplete();
        
        /**
         * 发生错误
         */
        void onError(Exception e);
    }
}


