package com.example.agenticorderservice.customer.api;

import static com.example.agenticorderservice.shared.api.ApiError.Code.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.agenticorderservice.customer.application.*;
import com.example.agenticorderservice.shared.api.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CustomerApiTest extends ApiTestSupport {
    @Test
    void normalizesUnicodeNameAndAsciiEmailAndPreservesPlusDots() throws Exception {
        mvc.perform(post("/customers")
                        .contentType("application/json;charset=UTF-8")
                        .content("""
                {"name":"  Älice😀  ","email":" USER.Name+Tag@Example.COM "}
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Älice😀"))
                .andExpect(jsonPath("$.email").value("user.name+tag@example.com"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "bad", "ü@example.com", "a..b@example.com", "a@-example.com"})
    void rejectsInvalidEmails(String email) throws Exception {
        error(
                post("/customers")
                        .contentType("application/json")
                        .content("{\"name\":\"Valid\",\"email\":"
                                + context.getBean(tools.jackson.databind.json.JsonMapper.class)
                                        .writeValueAsString(email)
                                + "}"),
                INVALID_EMAIL);
        verify(customers, never()).create(any(), any());
    }

    @Test
    void notFoundAndApprovedDeleteHaveDistinctContracts() throws Exception {
        when(customers.get(id)).thenThrow(new CustomerNotFoundException());
        error(get("/customers/" + id), CUSTOMER_NOT_FOUND);
        mvc.perform(delete("/customers/" + id))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(customers).delete(id);
        doThrow(new CustomerNotFoundException()).when(customers).delete(id);
        error(delete("/customers/" + id), CUSTOMER_NOT_FOUND);
        doThrow(new CustomerHasOrdersException()).when(customers).delete(id);
        error(delete("/customers/" + id), CUSTOMER_HAS_ORDERS);
        error(delete("/products/" + id), METHOD_NOT_ALLOWED);
        verify(customers, never()).deactivate(any());
    }
}
