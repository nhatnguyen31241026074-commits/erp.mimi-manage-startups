package com.techforge.erp.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RoleInterceptor roleInterceptor;

    @Autowired
    public WebConfig(RoleInterceptor roleInterceptor) {
        this.roleInterceptor = roleInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(roleInterceptor)
                .addPathPatterns("/api/v1/**") // Áp dụng cho toàn bộ API
                // Loại trừ các API không cần check quyền
                .excludePathPatterns(
                        "/api/v1/auth/**",      // Ví dụ: Login, Register, Refresh Token
                        "/api/v1/public/**",    // Các API public khác (nếu có)
                        "/swagger-ui/**",       // Swagger UI
                        "/v3/api-docs/**",      // OpenAPI docs
                        "/error"                // Spring error endpoint
                );
    }
}