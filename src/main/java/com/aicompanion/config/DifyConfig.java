package com.aicompanion.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Dify AI 平台配置
 */
@Slf4j
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
    private int readTimeout = 60000;

    /**
     * 最大连接池大小
     */
    private int maxPoolSize = 20;

    /**
     * 每个路由最大连接数
     */
    private int maxPerRoute = 10;

    @Data
    public static class Apps {
        private AppKey exam = new AppKey();
        private AppKey resume = new AppKey();
    }

    @Data
    public static class AppKey {
        private String apiKey;
    }

    /**
     * 启动时校验必要配置（fail-fast）
     */
    @PostConstruct
    public void validate() {
        if (baseUrl == null || baseUrl.isBlank() || "http://localhost/v1".equals(baseUrl)) {
            log.warn("Dify base-url 使用默认值 http://localhost/v1，部署时请设置 DIFY_BASE_URL");
        }
        if (apps.exam.apiKey == null || apps.exam.apiKey.isBlank()) {
            log.warn("Dify exam API Key 未配置，启动后考核相关接口不可用");
        }
        if (apps.resume.apiKey == null || apps.resume.apiKey.isBlank()) {
            log.warn("Dify resume API Key 未配置，启动后简历优化接口不可用");
        }
        log.info("Dify 配置加载完成: baseUrl={}, 连接池={}/路由, 超时=connect:{}ms/read:{}ms",
                baseUrl, maxPerRoute, connectTimeout, readTimeout);
    }

    /**
     * 专用于 Dify API 调用的 RestTemplate（带连接池 + 合理超时）
     */
    @Bean("difyRestTemplate")
    public RestTemplate difyRestTemplate() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(maxPoolSize);
        connectionManager.setDefaultMaxPerRoute(maxPerRoute);

        // 用 RequestConfig 统一管理连接超时和 socket 读超时
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(connectTimeout))
                .setResponseTimeout(Timeout.ofMilliseconds(readTimeout))
                .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(
                HttpClients.custom()
                        .setConnectionManager(connectionManager)
                        .setDefaultRequestConfig(requestConfig)
                        .build());

        log.info("创建 Dify RestTemplate: 连接池={}, 超时=connect:{}ms/read:{}ms",
                maxPoolSize, connectTimeout, readTimeout);

        return new RestTemplate(factory);
    }
}
