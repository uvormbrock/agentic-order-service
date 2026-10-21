package com.example.agenticorderservice.shared.api;

import static org.mockito.Mockito.*;

import com.example.agenticorderservice.customer.application.*;
import com.example.agenticorderservice.product.application.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(
        properties = "spring.autoconfigure.exclude="
                + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration")
public abstract class ApiTestSupport {
    // The retained concrete compatibility mock also implements the list input contract.
    @Autowired
    protected com.example.agenticorderservice.product.application.port.in.ListProductsQuery listProducts;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    protected com.example.agenticorderservice.order.application.port.out.OrderReadStore orderReadStore;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    protected com.example.agenticorderservice.order.application.port.in.OrderPlacementOperations placement;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    protected com.example.agenticorderservice.order.application.port.out.OrderWriteStore orderWriteStore;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    protected com.example.agenticorderservice.order.application.port.out.OrderPlacementAttempts orderPlacementAttempts;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    protected com.example.agenticorderservice.order.application.port.in.OrderQueries orderQueries;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    protected com.example.agenticorderservice.customer.application.CustomerOrderAccess customerOrderAccess;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    protected com.example.agenticorderservice.product.application.ProductOrderAccess productOrderAccess;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    protected com.example.agenticorderservice.order.application.OrderReferenceQuery orderReferences;

    @org.springframework.test.context.bean.override.mockito.MockitoBean(name = "orderPlacementTransactionTemplate")
    protected org.springframework.transaction.support.TransactionTemplate orderPlacementTransactions;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    protected com.example.agenticorderservice.customer.application.port.out.CustomerReadStore customerReads;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    protected com.example.agenticorderservice.customer.application.port.out.CustomerWriteStore customerWrites;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    protected com.example.agenticorderservice.product.application.port.out.ProductImageMetadataStore imageMetadata;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    protected com.example.agenticorderservice.product.application.port.out.ProductImageTransactions imageTransactions;

    @Autowired
    protected WebApplicationContext context;

    @MockitoBean
    protected com.example.agenticorderservice.product.application.port.in.ProductImageOperations productImages;

    @MockitoBean
    protected ProductApplicationService products;

    @MockitoBean
    protected com.example.agenticorderservice.product.application.port.in.GetProductQuery getProduct;

    @MockitoBean
    protected com.example.agenticorderservice.product.application.port.out.FindProductById productLookup;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    protected com.example.agenticorderservice.product.application.port.in.ProductWriteOperations productWrites;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    protected com.example.agenticorderservice.product.application.port.out.ProductWriteStore productWriteStore;

    @MockitoBean
    protected com.example.agenticorderservice.customer.application.port.in.CustomerOperations customers;

    protected MockMvc mvc;
    protected final UUID id = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    @BeforeEach
    void configureMvcAndServices() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        when(productWrites.create(anyString(), any()))
                .thenAnswer(c -> new ProductResult(id, c.getArgument(0), c.getArgument(1), true));
        when(customers.create(anyString(), anyString()))
                .thenAnswer(c -> new CustomerResult(id, c.getArgument(0), c.getArgument(1), true));
        when(getProduct.get(any())).thenReturn(new ProductResult(id, "Name", new BigDecimal("12.34"), false));
        when(customers.get(any())).thenReturn(new CustomerResult(id, "Name", "user@example.com", false));
        when(productWrites.update(any(), anyString(), any()))
                .thenAnswer(c -> new ProductResult(id, c.getArgument(1), c.getArgument(2), false));
        when(customers.update(any(), anyString(), anyString()))
                .thenAnswer(c -> new CustomerResult(id, c.getArgument(1), c.getArgument(2), false));
        when(productWrites.deactivate(any())).thenReturn(new ProductResult(id, "Name", new BigDecimal("12.34"), false));
        when(customers.deactivate(any())).thenReturn(new CustomerResult(id, "Name", "user@example.com", false));
        when(listProducts.list()).thenReturn(List.of());
        when(customers.list()).thenReturn(List.of());
    }

    protected void error(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request, ApiError.Code code)
            throws Exception {
        var response = mvc.perform(request).andReturn().getResponse();
        org.assertj.core.api.Assertions.assertThat(response.getStatus()).isEqualTo(code.status());
        org.assertj.core.api.Assertions.assertThat(response.getContentType()).startsWith("application/json");
        org.assertj.core.api.Assertions.assertThat(response.getContentAsString())
                .isEqualTo(context.getBean(tools.jackson.databind.json.JsonMapper.class)
                        .writeValueAsString(new ApiError(code)));
        ExpectedApiErrors.assertResponse(response.getStatus(), response.getContentAsString(), code.name());
    }
}
