package com.example.agenticorderservice.customer.api;
import com.example.agenticorderservice.shared.api.*;
import java.util.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static com.example.agenticorderservice.shared.api.ApiError.Code.*;
class CustomerApiIT extends PostgreSqlApiTestSupport {
    @Test void executesAllFiveEndpointsAgainstCommittedPostgreSqlState() throws Exception {
        Object value=UUID.randomUUID()+"+tag@example.com";
        var created=request("POST","/customers",mapper.writeValueAsString(Map.of("name","  Älice😀  ","email",value)));
        assertThat(created.statusCode()).isEqualTo(201);var id=json(created).get("id").asString();
        assertThat(created.headers().firstValue("Location")).contains("/customers/"+id);
        assertThat(json(created).size()).isEqualTo(4);assertThat(json(created).has("version")).isFalse();
        assertThat(jdbc.queryForObject("select version from customers where id=?",Long.class,UUID.fromString(id))).isZero();
        var firstPatch=request("PATCH","/customers/"+id,"{\"active\":false}");assertThat(firstPatch.statusCode()).isEqualTo(200);
        Long version=jdbc.queryForObject("select version from customers where id=?",Long.class,UUID.fromString(id));
        var stats=factory.unwrap(org.hibernate.SessionFactory.class).getStatistics();stats.clear();
        var repeat=request("PATCH","/customers/"+id,"{\"active\":false}");
        assertThat(repeat.statusCode()).isEqualTo(200);assertThat(repeat.body()).isEqualTo(firstPatch.body());
        assertThat(stats.getEntityUpdateCount()).isZero();
        assertThat(jdbc.queryForObject("select version from customers where id=?",Long.class,UUID.fromString(id))).isEqualTo(version);
        Object changed=UUID.randomUUID()+"@example.com";
        var updated=request("PUT","/customers/"+id,mapper.writeValueAsString(Map.of("name","Changed","email",changed)));
        assertThat(updated.statusCode()).isEqualTo(200);assertThat(json(updated).get("active").asBoolean()).isFalse();
        assertThat(jdbc.queryForObject("select name from customers where id=?",String.class,UUID.fromString(id))).isEqualTo("Changed");
        assertThat(jdbc.queryForObject("select version from customers where id=?",Long.class,UUID.fromString(id))).isEqualTo(version+1);
        stats.clear();var found=request("GET","/customers/"+id,null);assertThat(found.statusCode()).isEqualTo(200);assertThat(found.body()).isEqualTo(updated.body());
        assertThat(stats.getPrepareStatementCount()).isEqualTo(1); // Full materialization: serialization adds no query.
        var listed=request("GET","/customers",null);assertThat(listed.statusCode()).isEqualTo(200);
        assertThat(json(listed)).anySatisfy(row -> { assertThat(row.size()).isEqualTo(4); });
        assertError(request("PUT","/customers/"+id,"{\"name\":\"Unwanted\",\"email\":null}"),REQUIRED_FIELD_MISSING);
        assertThat(request("GET","/customers/"+id,null).body()).isEqualTo(updated.body());
        assertError(request("GET","/customers/"+UUID.randomUUID(),null),CUSTOMER_NOT_FOUND);
    }

    @Test void enforcesInactiveUniquenessAtActualTransactionCompletionAndAllowsSameOwner() throws Exception {
        String email=UUID.randomUUID()+"@example.com";
        var created=request("POST","/customers",mapper.writeValueAsString(Map.of("name","Owner","email",email)));
        String id=json(created).get("id").asString();request("PATCH","/customers/"+id,"{\"active\":false}");
        long before=jdbc.queryForObject("select count(*) from customers",Long.class);
        assertError(request("POST","/customers",mapper.writeValueAsString(Map.of("name","Duplicate","email",email.toUpperCase(Locale.ROOT)))),DUPLICATE_EMAIL);
        assertThat(jdbc.queryForObject("select count(*) from customers",Long.class)).isEqualTo(before);
        var own=request("PUT","/customers/"+id,mapper.writeValueAsString(Map.of("name","Owner Changed","email",email.toUpperCase(Locale.ROOT))));
        assertThat(own.statusCode()).isEqualTo(200);
        var other=request("POST","/customers",mapper.writeValueAsString(Map.of("name","Other","email",UUID.randomUUID()+"@example.com")));
        String otherId=json(other).get("id").asString();
        assertError(request("PUT","/customers/"+otherId,mapper.writeValueAsString(Map.of("name","Unwanted","email",email))),DUPLICATE_EMAIL);
        assertThat(request("GET","/customers/"+otherId,null).body()).isEqualTo(other.body());
        assertError(request("DELETE","/customers/"+id,null),OPERATION_NOT_AVAILABLE);
        assertThat(request("GET","/customers/"+id,null).body()).isEqualTo(own.body());
    }
}
