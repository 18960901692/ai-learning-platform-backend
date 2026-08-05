package com.aicompanion.config;

import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPooled;

/**
 * RAG 配置类
 *
 * 手动配置 RedisVectorStore（Spring AI 1.0.0 版本需要手动创建）
 * 配置 QuestionAnswerAdvisor，用于自动检索向量库并注入上下文
 */
@Configuration
public class RagConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    /**
     * Jedis 连接池
     */
    @Bean
    public JedisPooled jedisPooled() {
        return new JedisPooled(redisHost, redisPort);
    }

    /**
     * Redis 向量库（持久化，重启不丢失）
     */
    @Bean
    public VectorStore vectorStore(JedisPooled jedisPooled, EmbeddingModel embeddingModel) {
        return RedisVectorStore.builder(jedisPooled, embeddingModel)
                .indexName("knowledge-index")
                .prefix("knowledge:")
                .initializeSchema(true)
                .build();
    }

    /**
     * RAG 增强器：每次提问自动检索 Top5 相似片段，注入到 Prompt 中
     */
    @Bean
    public QuestionAnswerAdvisor questionAnswerAdvisor(VectorStore vectorStore) {
        return QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder()
                        .topK(5)
                        .similarityThreshold(0.3)
                        .build())
                .build();
    }
}
