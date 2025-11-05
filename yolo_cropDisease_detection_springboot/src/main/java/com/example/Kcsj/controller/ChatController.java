package com.example.Kcsj.controller;

import com.example.Kcsj.common.Result;
import com.example.Kcsj.dto.LlmChatMessage;
import com.example.Kcsj.dto.LlmChatRequest;
import com.example.Kcsj.dto.LlmChatResponse;
import com.example.Kcsj.service.SparkLlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天控制器
 * 提供与讯飞星火大模型的交互接口
 */
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {
    
    private final SparkLlmService sparkLlmService;
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * 会话上下文管理（sessionId -> 消息历史）
     * 实际生产环境应使用Redis等分布式缓存
     */
    private final Map<String, List<LlmChatMessage>> sessionContextMap = new ConcurrentHashMap<>();
    
    /**
     * 发送聊天消息（HTTP同步方式）
     * POST /api/chat/send
     */
    @PostMapping("/send")
    public Result<?> sendMessage(@RequestBody LlmChatRequest request) {
        try {
            // 生成会话ID（如果没有提供）
            if (request.getSessionId() == null) {
                request.setSessionId(UUID.randomUUID().toString());
            }
            
            // 获取历史上下文
            List<LlmChatMessage> history = sessionContextMap.getOrDefault(
                    request.getSessionId(), 
                    new ArrayList<>()
            );
            request.setHistory(history);
            
            // 调用LLM服务
            LlmChatResponse response = sparkLlmService.sendMessage(request);
            
            // 更新会话上下文
            updateSessionContext(request.getSessionId(), request.getMessage(), response.getContent());
            
            return Result.success(response);
        } catch (Exception e) {
            log.error("发送聊天消息失败", e);
            return Result.error("-1", "聊天失败：" + e.getMessage());
        }
    }
    
    /**
     * WebSocket 消息处理（流式响应）
     * 客户端通过 WebSocket 发送消息到 /app/chat/stream
     * 服务端通过 /topic/chat/{sessionId} 推送流式响应
     */
    @MessageMapping("/chat/stream")
    public void sendMessageStream(LlmChatRequest request) {
        try {
            log.info("收到WebSocket聊天请求：{}", request.getMessage());
            
            // 生成会话ID（如果没有提供）
            if (request.getSessionId() == null) {
                request.setSessionId(UUID.randomUUID().toString());
            }
            
            String sessionId = request.getSessionId();
            
            // 获取历史上下文
            List<LlmChatMessage> history = sessionContextMap.getOrDefault(sessionId, new ArrayList<>());
            request.setHistory(history);
            request.setStream(true);
            
            // 存储完整响应（用于更新上下文）
            StringBuilder fullResponse = new StringBuilder();
            
            // 调用LLM服务（流式）
            sparkLlmService.sendMessageStream(request, new SparkLlmService.StreamCallback() {
                @Override
                public void onMessage(String content) {
                    // 推送流式消息片段到客户端
                    LlmChatResponse response = LlmChatResponse.builder()
                            .sessionId(sessionId)
                            .content(content)
                            .isEnd(false)
                            .status(0)
                            .build();
                    
                    messagingTemplate.convertAndSend("/topic/chat/" + sessionId, response);
                    fullResponse.append(content);
                }
                
                @Override
                public void onComplete() {
                    // 发送完成消息
                    LlmChatResponse response = LlmChatResponse.builder()
                            .sessionId(sessionId)
                            .content("")
                            .isEnd(true)
                            .status(0)
                            .build();
                    
                    messagingTemplate.convertAndSend("/topic/chat/" + sessionId, response);
                    
                    // 更新会话上下文
                    updateSessionContext(sessionId, request.getMessage(), fullResponse.toString());
                    
                    log.info("WebSocket流式响应完成，会话ID: {}", sessionId);
                }
                
                @Override
                public void onError(Exception e) {
                    log.error("WebSocket流式响应失败", e);
                    
                    // 发送错误消息
                    LlmChatResponse response = LlmChatResponse.builder()
                            .sessionId(sessionId)
                            .content("")
                            .isEnd(true)
                            .status(-1)
                            .errorMessage(e.getMessage())
                            .build();
                    
                    messagingTemplate.convertAndSend("/topic/chat/" + sessionId, response);
                }
            });
            
        } catch (Exception e) {
            log.error("处理WebSocket聊天请求失败", e);
        }
    }
    
    /**
     * 获取会话历史
     * GET /api/chat/history/{sessionId}
     */
    @GetMapping("/history/{sessionId}")
    public Result<?> getChatHistory(@PathVariable String sessionId) {
        try {
            List<LlmChatMessage> history = sessionContextMap.getOrDefault(sessionId, new ArrayList<>());
            return Result.success(history);
        } catch (Exception e) {
            log.error("获取聊天历史失败", e);
            return Result.error("-1", "获取历史失败：" + e.getMessage());
        }
    }
    
    /**
     * 清空会话历史
     * DELETE /api/chat/history/{sessionId}
     */
    @DeleteMapping("/history/{sessionId}")
    public Result<?> clearChatHistory(@PathVariable String sessionId) {
        try {
            sessionContextMap.remove(sessionId);
            return Result.success("会话历史已清空");
        } catch (Exception e) {
            log.error("清空聊天历史失败", e);
            return Result.error("-1", "清空失败：" + e.getMessage());
        }
    }
    
    /**
     * 更新会话上下文
     */
    private void updateSessionContext(String sessionId, String userMessage, String assistantMessage) {
        List<LlmChatMessage> history = sessionContextMap.computeIfAbsent(sessionId, k -> new ArrayList<>());
        
        // 添加用户消息
        history.add(LlmChatMessage.builder()
                .role("user")
                .content(userMessage)
                .build());
        
        // 添加助手消息
        history.add(LlmChatMessage.builder()
                .role("assistant")
                .content(assistantMessage)
                .build());
        
        // 限制历史消息数量（最多保留最近10轮对话）
        if (history.size() > 20) {
            history.subList(0, history.size() - 20).clear();
        }
        
        log.debug("更新会话上下文，会话ID: {}, 历史消息数: {}", sessionId, history.size());
    }
}
