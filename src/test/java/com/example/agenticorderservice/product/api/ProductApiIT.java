package com.example.agenticorderservice.product.api;
import com.example.agenticorderservice.shared.api.*;
import java.util.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static com.example.agenticorderservice.shared.api.ApiError.Code.*;
class ProductApiIT extends PostgreSqlApiTestSupport {
    @Test void executesAllFiveEndpointsAgainstCommittedPostgreSqlState() throws Exception {
        Object value=new BigDecimal("12.34");
        var created=request("POST","/products",mapper.writeValueAsString(Map.of("name","  Älice😀  ","price",value)));
        assertThat(created.statusCode()).isEqualTo(201);var id=json(created).get("id").asString();
        assertThat(created.headers().firstValue("Location")).contains("/products/"+id);
        assertThat(json(created).size()).isEqualTo(4);assertThat(json(created).has("version")).isFalse();
        assertThat(jdbc.queryForObject("select version from products where id=?",Long.class,UUID.fromString(id))).isZero();
        var firstPatch=request("PATCH","/products/"+id,"{\"active\":false}");assertThat(firstPatch.statusCode()).isEqualTo(200);
        Long version=jdbc.queryForObject("select version from products where id=?",Long.class,UUID.fromString(id));
        var stats=factory.unwrap(org.hibernate.SessionFactory.class).getStatistics();stats.clear();
        var repeat=request("PATCH","/products/"+id,"{\"active\":false}");
        assertThat(repeat.statusCode()).isEqualTo(200);assertThat(repeat.body()).isEqualTo(firstPatch.body());
        assertThat(stats.getEntityUpdateCount()).isZero();
        assertThat(jdbc.queryForObject("select version from products where id=?",Long.class,UUID.fromString(id))).isEqualTo(version);
        Object changed=new BigDecimal("45.67");
        var updated=request("PUT","/products/"+id,mapper.writeValueAsString(Map.of("name","Changed","price",changed)));
        assertThat(updated.statusCode()).isEqualTo(200);assertThat(json(updated).get("active").asBoolean()).isFalse();
        assertThat(jdbc.queryForObject("select name from products where id=?",String.class,UUID.fromString(id))).isEqualTo("Changed");
        assertThat(jdbc.queryForObject("select version from products where id=?",Long.class,UUID.fromString(id))).isEqualTo(version+1);
        stats.clear();var found=request("GET","/products/"+id,null);assertThat(found.statusCode()).isEqualTo(200);assertThat(found.body()).isEqualTo(updated.body());
        assertThat(stats.getPrepareStatementCount()).isEqualTo(1); // Full materialization: serialization adds no query.
        var listed=request("GET","/products",null);assertThat(listed.statusCode()).isEqualTo(200);
        assertThat(json(listed)).anySatisfy(row -> { assertThat(row.size()).isEqualTo(4); });
        assertError(request("PUT","/products/"+id,"{\"name\":\"Unwanted\",\"price\":null}"),REQUIRED_FIELD_MISSING);
        assertThat(request("GET","/products/"+id,null).body()).isEqualTo(updated.body());
        assertError(request("GET","/products/"+UUID.randomUUID(),null),PRODUCT_NOT_FOUND);
    }
}
