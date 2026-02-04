package com.evho.usonly.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 모든 주소 허용
                .allowedOrigins("*") // 모든 출처 허용 (보안상 나중엔 특정 주소만 허용해야 함)
                .allowedMethods("GET", "POST", "PUT", "DELETE"); // 모든 방식 허용
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**") // url에 /images/로 시작하면
                .addResourceLocations("file:///C:/usonly_images/"); // C드라이브 폴더에서 찾아라
    }
}