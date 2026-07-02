package com.aicompanion.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI ChatClient 配置
 */
@Configuration
public class AiChatConfig {

    /**
     * 配置 AI 伴学助手的系统提示词
     */
    private static final String SYSTEM_PROMPT = """
            你是"AI伴学助手"，一个面向大学生的智能学习伴侣。
            你的职责：
            1. 回答技术问题（Java、Spring、数据库、前端等）
            2. 解释概念时用通俗易懂的语言，配合代码示例
            3. 鼓励学生，保持友好积极的语气
            4. 如果不确定，坦诚说明，不要编造答案
            5. 当用户询问技能相关问题时，使用工具查询数据库中的实际数据
            """;

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }
}
