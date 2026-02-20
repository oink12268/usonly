package com.evho.usonly.global.config;

import com.evho.usonly.global.filter.FirebaseAuthFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<FirebaseAuthFilter> firebaseAuthFilterRegistration() {
        FilterRegistrationBean<FirebaseAuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new FirebaseAuthFilter());
        registration.addUrlPatterns("/*"); // shouldNotFilter로 /api/** 만 적용됨
        registration.setOrder(1);
        return registration;
    }
}
