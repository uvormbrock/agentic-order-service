package com.example.agenticorderservice;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(
        properties = "spring.autoconfigure.exclude="
                + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration")
class AgenticOrderServiceApplicationTest {

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.example.agenticorderservice.product.application.ProductApplicationService products;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.example.agenticorderservice.customer.application.CustomerApplicationService customers;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    protected com.example.agenticorderservice.order.application.OrderPlacementCoordinator placement;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    protected com.example.agenticorderservice.order.application.OrderQueryService orderQueries;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    protected com.example.agenticorderservice.customer.application.CustomerOrderAccess customerOrderAccess;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    protected com.example.agenticorderservice.product.application.ProductOrderAccess productOrderAccess;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    protected com.example.agenticorderservice.order.application.OrderReferenceQuery orderReferences;

    @org.springframework.test.context.bean.override.mockito.MockitoBean(name = "orderPlacementTransactionTemplate")
    protected org.springframework.transaction.support.TransactionTemplate orderPlacementTransactions;

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoadsWithWebMvcAndValidation() {
        assertThat(context).isInstanceOf(WebApplicationContext.class);
        assertThat(context.containsBean("dispatcherServlet")).isTrue();
        assertThat(context.getBean(Validator.class)).isNotNull();
    }
}
