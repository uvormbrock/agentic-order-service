package com.example.agenticorderservice.order.api;

import com.example.agenticorderservice.shared.api.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static com.example.agenticorderservice.shared.api.ApiError.Code.*;

class OrderApiIT extends PostgreSqlApiTestSupport {
    UUID customer() throws Exception {
        var response=request("POST","/customers",mapper.writeValueAsString(Map.of("name","Current","email",UUID.randomUUID()+"@example.com")));
        assertThat(response.statusCode()).isEqualTo(201);return UUID.fromString(json(response).get("id").asString());
    }
    UUID product(String price) throws Exception {
        var response=request("POST","/products",mapper.writeValueAsString(Map.of("name","Historical","price",new java.math.BigDecimal(price))));
        assertThat(response.statusCode()).isEqualTo(201);return UUID.fromString(json(response).get("id").asString());
    }
    String body(UUID customer,UUID product,int quantity) {return mapper.writeValueAsString(Map.of("customerId",customer,"items",List.of(Map.of("productId",product,"quantity",quantity))));}
    @Test void commitsOwnedItemsAndRetrievesOriginalAmountsWithCurrentInactiveCustomerInTwoQueries() throws Exception {
        UUID customer=customer(),product=product("100.05");
        var created=request("POST","/orders",body(customer,product,1));assertThat(created.statusCode()).isEqualTo(201);
        var tree=json(created);UUID order=UUID.fromString(tree.get("id").asString());assertThat(created.headers().firstValue("Location")).contains("/orders/"+order);
        assertThat(tree.propertyNames()).containsExactlyInAnyOrder("id","customerId","customer","items","currency","subtotal","discount","total");
        assertThat(tree.get("discount").decimalValue()).isEqualByComparingTo("10.01");assertThat(tree.get("total").decimalValue()).isEqualByComparingTo("90.04");
        assertThat(jdbc.queryForObject("select count(*) from order_items where order_id=?",Integer.class,order)).isEqualTo(1);
        assertThat(request("PUT","/products/"+product,"{\"name\":\"Changed\",\"price\":1}").statusCode()).isEqualTo(200);
        assertThat(request("PATCH","/products/"+product,"{\"active\":false}").statusCode()).isEqualTo(200);
        var updated=request("PUT","/customers/"+customer,mapper.writeValueAsString(Map.of("name","Changed customer","email",UUID.randomUUID()+"@example.com")));assertThat(updated.statusCode()).isEqualTo(200);
        var deleted=request("DELETE","/customers/"+customer,null);assertThat(deleted.statusCode()).isEqualTo(204);assertThat(deleted.body()).isEmpty();
        long version=jdbc.queryForObject("select version from customers where id=?",Long.class,customer);
        assertThat(request("DELETE","/customers/"+customer,null).statusCode()).isEqualTo(204);
        assertThat(jdbc.queryForObject("select version from customers where id=?",Long.class,customer)).isEqualTo(version);
        var stats=factory.unwrap(org.hibernate.SessionFactory.class).getStatistics();stats.clear();
        var read=request("GET","/orders/"+order,null);assertThat(read.statusCode()).isEqualTo(200);
        assertThat(json(read).get("customer").get("name").asString()).isEqualTo("Changed customer");assertThat(json(read).get("customer").get("active").asBoolean()).isFalse();
        assertThat(json(read).get("items")).isEqualTo(tree.get("items"));assertThat(json(read).get("total")).isEqualTo(tree.get("total"));
        assertThat(stats.getPrepareStatementCount()).isEqualTo(2);
        stats.clear();var list=request("GET","/orders",null);assertThat(list.statusCode()).isEqualTo(200);assertThat(stats.getPrepareStatementCount()).isEqualTo(2);
        boolean found=false;for(var value:json(list)) {if(value.get("id").asString().equals(order.toString()))found=true;}assertThat(found).isTrue();
        assertError(request("POST","/orders",body(customer,product,1)),CUSTOMER_INACTIVE);
        assertThat(jdbc.queryForObject("select count(*) from orders where customer_id=?",Integer.class,customer)).isEqualTo(1);
    }
    @Test void consolidatesBoundaryAndRejectsInvalidOrMissingResourcesWithoutRows() throws Exception {
        UUID customer=customer(),product=product("0");
        String body=mapper.writeValueAsString(Map.of("customerId",customer,"items",List.of(Map.of("productId",product,"quantity",6000),Map.of("productId",product,"quantity",4000))));
        var created=request("POST","/orders",body);assertThat(created.statusCode()).isEqualTo(201);assertThat(json(created).get("items").size()).isEqualTo(1);
        assertThat(json(created).get("items").get(0).get("quantity").asInt()).isEqualTo(10000);
        assertError(request("POST","/orders",body.replace("4000","4001")),INVALID_ORDER_QUANTITY);
        assertError(request("POST","/orders",body(UUID.randomUUID(),product,1)),CUSTOMER_NOT_FOUND);
        assertError(request("POST","/orders",body(customer,UUID.randomUUID(),1)),PRODUCT_NOT_FOUND);
        assertThat(request("PATCH","/products/"+product,"{\"active\":false}").statusCode()).isEqualTo(200);
        assertError(request("POST","/orders",body(customer,product,1)),PRODUCT_INACTIVE);
        assertThat(jdbc.queryForObject("select count(*) from orders where customer_id=?",Integer.class,customer)).isEqualTo(1);
        UUID disposable=customer();assertThat(request("DELETE","/customers/"+disposable,null).statusCode()).isEqualTo(204);
        assertError(request("DELETE","/customers/"+disposable,null),CUSTOMER_NOT_FOUND);
        assertError(request("GET","/orders/"+UUID.randomUUID(),null),ORDER_NOT_FOUND);
        assertThat(jdbc.queryForObject("select count(*) from flyway_schema_history where success",Integer.class)).isEqualTo(2);
    }
}
