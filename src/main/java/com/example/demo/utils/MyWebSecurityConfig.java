package com.example.demo.utils;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * 禁用 CSRF 保护并启用 CORS。
 */
@Configuration
public class MyWebSecurityConfig extends WebSecurityConfigurerAdapter {
    
    /**
     * 禁用 CSRF 保护并启用 CORS。
     *
     * @param http 这是用于配置 Web 安全性的主界面。
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
                .cors().and();
    }
}
