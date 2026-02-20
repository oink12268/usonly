package com.evho.usonly.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${custom.file.dir}") // 경로 주입
    private String uploadDir;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 모든 주소 허용
                .allowedOriginPatterns("http://localhost:*", "http://usonly.iptime.org:*", "http://192.168.0.*:*")
                .allowedMethods("GET", "POST", "PUT", "DELETE"); // 모든 방식 허용
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**") // url에 /images/로 시작하면
                .addResourceLocations("file:///" + uploadDir); // 폴더에서 찾아라
    }
}