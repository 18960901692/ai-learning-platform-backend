package com.aicompanion.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Dify AI 平台配置
 */
@Configuration
@ConfigurationProperties(prefix = "dify")
@Data
public class DifyConfig {

    /**
     * Dify API 基础地址（如 http://localhost/v1）
     */
    private String baseUrl;

    /**
     * 各 Dify 应用的 API Key 配置
     */
    private Apps apps = new Apps();

    /**
     * 连接超时（毫秒）
     */
    private int connectTimeout = 10000;

    /**
     * 读取超时（毫秒）
     */
    private int readTimeout = 120000;

    @Data
    public static class Apps {
        /**
         * 考核类 Dify App（出题/对话/阅卷共用一个 App）
         */
        private AppKey exam = new AppKey();

        /**
         * 简历优化 Dify Workflow
         */
        private AppKey resume = new AppKey();
    }

    @Data
    public static class AppKey {
        private String apiKey;
    }

    /**
     * 专用于 Dify API 调用的 RestTemplate
     */
    @Bean("difyRestTemplate")
    public RestTemplate difyRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return new RestTemplate(factory);
    }
}
