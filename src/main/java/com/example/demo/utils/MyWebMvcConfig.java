package com.example.demo.utils;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 该类是一个配置类，实现了WebMvcConfigurer接口，重写了addCorsMappings方法
 */
@Configuration
public class MyWebMvcConfig implements WebMvcConfigurer {
    
    /**
     * 允许从 localhost:3000 域到 /api/** 端点的所有请求的所有标头、所有方法和最长 30 分钟的时间。
     *
     * @param registry 要修改的 CorsRegistry
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry
                .addMapping("/api/**")
                .allowedHeaders("*")
                .allowedMethods("*")
                .maxAge(1800)
                .allowedOrigins("http://localhost:3000");
    }
}
