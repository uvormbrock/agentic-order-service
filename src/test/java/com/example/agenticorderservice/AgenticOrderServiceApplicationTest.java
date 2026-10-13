package com.example.agenticorderservice;

import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AgenticOrderServiceApplicationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoadsWithWebMvcAndValidation() {
        assertThat(context).isInstanceOf(WebApplicationContext.class);
        assertThat(context.containsBean("dispatcherServlet")).isTrue();
        assertThat(context.getBean(Validator.class)).isNotNull();
    }
}
