package com.example.agenticorderservice.configuration;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.*;
import tools.jackson.databind.*;
@Configuration(proxyBeanMethods = false)
public class JacksonConfiguration {
    @Bean JsonMapperBuilderCustomizer strictScalarBinding() {
        return builder -> builder.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS);
    }
}
