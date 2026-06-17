package com.aicompanion.config;

import com.aicompanion.interceptor.JwtInterceptor;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    @Value("${file.upload.path}")
    private String uploadPath;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/**",              // 认证接口放行
                        "/error",                // 错误页面
                        "/swagger-ui/**",        // Swagger UI 资源
                        "/swagger-ui.html",      // Swagger UI 入口页
                        "/v3/api-docs/**",       // OpenAPI 文档
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/uploads/**"            // 静态资源放行
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolutePath = new java.io.File(uploadPath).getAbsolutePath();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absolutePath + "/");
    }

    /**
     * 获取上传目录的绝对路径
     */
    public String getUploadAbsolutePath() {
        return new java.io.File(uploadPath).getAbsolutePath();
    }

    /**
     * OpenAPI 配置
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI 伴学平台 API")
                        .version("1.0.0")
                        .description("AI Learning Platform Backend API Documentation"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT 认证，请输入 Token（无需 Bearer 前缀）")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
