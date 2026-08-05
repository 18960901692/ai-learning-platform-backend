package com.aicompanion.config;

import com.aicompanion.common.ai.RedisChatMemory;
import com.aicompanion.tool.LearningRecordTool;
import com.aicompanion.tool.SkillLookupTool;
import com.aicompanion.tool.UserSkillAnalysisTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * AI 配置类
 *
 * 知识点：
 * 1. 统一配置 ChatClient Bean，注入 ChatMemory 和 Tools
 * 2. MessageChatMemoryAdvisor 是 Spring AI 的记忆拦截器，自动读写 ChatMemory
 * 3. RedisChatMemory 用 Redis List 存储对话消息，持久化记忆，重启不丢失，自动过期
 * 4. defaultTools() 注册的工具对所有对话生效
 * 5. @Primary 标记 OpenAI(DeepSeek) ChatModel 为首选，避免与 DashScope ChatModel 冲突
 */
@Configuration
public class AiChatConfig {

    /**
     * 标记 OpenAI(DeepSeek) ChatModel 为 @Primary
     * 因为引入 DashScope(仅用 EmbeddingModel) 后会产生两个 ChatModel Bean
     */
    @Bean
    @Primary
    public ChatModel primaryChatModel(@Qualifier("openAiChatModel") ChatModel openAiChatModel) {
        return openAiChatModel;
    }

    /**
     * 注册 ChatMemory Bean（基于 Redis 实现）
     */
    @Bean
    public ChatMemory chatMemory(StringRedisTemplate redisTemplate) {
        return new RedisChatMemory(redisTemplate, 50);
    }

    /**
     * 注册 ChatClient Bean
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory,
                                  SkillLookupTool skillLookupTool,
                                  UserSkillAnalysisTool userSkillAnalysisTool,
                                  LearningRecordTool learningRecordTool) {
        return builder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(skillLookupTool, userSkillAnalysisTool, learningRecordTool)
                .build();
    }

    private static final String SYSTEM_PROMPT = """
            你是"AI伴学助手"，一个面向大学生的智能学习伴侣。
            你的职责：
            1. 回答技术问题（Java、Spring、数据库、前端等）
            2. 解释概念时用通俗易懂的语言，配合代码示例
            3. 鼓励学生，保持友好积极的语气
            4. 如果不确定，坦诚说明，不要编造答案
            5. 当用户询问技能相关问题时，使用工具查询数据库中的实际数据
            """;
}
