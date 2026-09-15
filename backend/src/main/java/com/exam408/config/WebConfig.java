package com.exam408.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                    "/api/auth/**",
                    "/api/public/**",
                    "/api/question/**",
                    "/api/knowledge-tree/**",
                    "/api/overview/**"
                );
    }
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/pdf/exam/**")
                .addResourceLocations("file:./2009-2025计算机408统考真题/");
        registry.addResourceHandler("/pdf/answer/**")
                .addResourceLocations("file:./2009-2025计算机408真题解析/");
        registry.addResourceHandler("/images/pages/**")
                .addResourceLocations("file:./images/pages/");
        registry.addResourceHandler("/images/questions/**")
                .addResourceLocations("classpath:/static/images/questions/");
    }
}
