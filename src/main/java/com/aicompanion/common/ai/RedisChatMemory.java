package com.aicompanion.common.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的对话记忆实现
 * 每个会话用一个 Redis List 存储消息，自动过期
 *
 * 知识点：
 * 1. ChatMemory 是 Spring AI 的对话记忆接口
 * 2. Message 是接口，Jackson 无法直接序列化，需要转为 role+content 格式存储
 * 3. 读取时根据 role 重建 UserMessage / AssistantMessage 对象
 */
public class RedisChatMemory implements ChatMemory {

    private static final String KEY_PREFIX = "ai:chat:";
    private static final long TTL_HOURS = 24;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final int maxMessages;

    public RedisChatMemory(StringRedisTemplate redisTemplate, int maxMessages) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.maxMessages = maxMessages;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        String key = KEY_PREFIX + conversationId;
        for (Message message : messages) {
            String json = serialize(message);
            if (!json.isEmpty()) {
                redisTemplate.opsForList().rightPush(key, json);
            }
        }
        Long size = redisTemplate.opsForList().size(key);
        if (size != null && size > maxMessages) {
            redisTemplate.opsForList().trim(key, size - maxMessages, size);
        }
        redisTemplate.expire(key, TTL_HOURS, TimeUnit.HOURS);
    }

    @Override
    public List<Message> get(String conversationId) {
        String key = KEY_PREFIX + conversationId;
        List<String> jsonList = redisTemplate.opsForList().range(key, 0, -1);
        if (jsonList == null) return new ArrayList<>();

        List<Message> messages = new ArrayList<>();
        for (String json : jsonList) {
            Message msg = deserialize(json);
            if (msg != null) messages.add(msg);
        }
        return messages;
    }

    @Override
    public void clear(String conversationId) {
        redisTemplate.delete(KEY_PREFIX + conversationId);
    }

    /**
     * 序列化：将 Message 转为 {"role":"USER","content":"..."} 格式
     * Message 是接口，不能直接用 Jackson 序列化，需要提取 role 和 content
     */
    private String serialize(Message message) {
        try {
            Map<String, String> map = new HashMap<>();
            map.put("role", message.getMessageType().getValue());
            map.put("content", message.getText());
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 反序列化：根据 role 重建 Message 对象
     * USER → UserMessage, ASSISTANT → AssistantMessage
     */
    private Message deserialize(String json) {
        try {
            Map<String, String> map = objectMapper.readValue(json, Map.class);
            String role = map.get("role");
            String content = map.get("content");
            if ("user".equalsIgnoreCase(role)) {
                return new UserMessage(content);
            } else if ("assistant".equalsIgnoreCase(role)) {
                return new AssistantMessage(content);
            }
            return null;
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
