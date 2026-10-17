package com.example.agenticorderservice.configuration;

import com.example.agenticorderservice.shared.api.*;
import jakarta.servlet.DispatcherType;
import java.util.EnumSet;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.core.Ordered;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.web.servlet.config.annotation.*;

@Configuration(proxyBeanMethods = false)
public class RequestLoggingConfiguration implements WebMvcConfigurer {
    private final ObjectProvider<AsyncTaskExecutor> executor;

    public RequestLoggingConfiguration(
            @Qualifier("applicationTaskExecutor") ObjectProvider<AsyncTaskExecutor> executor) {
        this.executor = executor;
    }

    @Bean
    ServletListenerRegistrationBean<RequestLoggingListener> requestLoggingListener() {
        return new ServletListenerRegistrationBean<>(new RequestLoggingListener());
    }

    @Bean
    FilterRegistrationBean<RequestCorrelationFilter> requestCorrelationFilter() {
        var registration = new FilterRegistrationBean<>(new RequestCorrelationFilter());
        registration.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR));
        registration.setAsyncSupported(true);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setTaskExecutor(new CorrelatedTaskExecutor(executor.getObject()));
    }
}
