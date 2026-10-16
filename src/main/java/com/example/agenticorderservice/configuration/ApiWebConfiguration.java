package com.example.agenticorderservice.configuration;

import com.example.agenticorderservice.shared.api.*;
import java.lang.reflect.Type;
import org.springframework.boot.http.converter.autoconfigure.ServerHttpMessageConvertersCustomizer;
import org.springframework.context.annotation.*;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

@Configuration(proxyBeanMethods = false)
public class ApiWebConfiguration implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ApiRequestInterceptor());
    }

    @Bean
    ServerHttpMessageConvertersCustomizer strictRequestConverter(StrictJsonRequestDecoder decoder) {
        return builder -> builder.addCustomConverter(new StrictRequestHttpMessageConverter(decoder));
    }

    @ControllerAdvice
    static class MissingBodyAdvice extends RequestBodyAdviceAdapter {
        private final StrictJsonRequestDecoder decoder;

        MissingBodyAdvice(StrictJsonRequestDecoder decoder) {
            this.decoder = decoder;
        }

        @Override
        public boolean supports(
                MethodParameter parameter, Type type, Class<? extends HttpMessageConverter<?>> converter) {
            return type instanceof Class<?> dto && decoder.supports(dto);
        }

        @Override
        public Object handleEmptyBody(
                Object body,
                HttpInputMessage input,
                MethodParameter parameter,
                Type type,
                Class<? extends HttpMessageConverter<?>> converter) {
            throw new ApiInputException(ApiError.Code.REQUIRED_FIELD_MISSING);
        }
    }
}
