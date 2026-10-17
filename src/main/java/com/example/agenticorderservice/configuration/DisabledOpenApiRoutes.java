package com.example.agenticorderservice.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.*;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.*;

/** Keep documentation and bundled Swagger assets unavailable outside the explicit demo profile. */
@Configuration(proxyBeanMethods = false)
@Profile("!demo")
public class DisabledOpenApiRoutes implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
                    @Override
                    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
                            throws java.io.IOException {
                        response.sendError(HttpServletResponse.SC_NOT_FOUND);
                        return false;
                    }
                })
                .addPathPatterns(
                        "/v3/api-docs",
                        "/v3/api-docs/**",
                        "/v3/api-docs.yaml",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/webjars/swagger-ui/**");
    }
}
