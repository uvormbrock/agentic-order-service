package com.example.agenticorderservice.customer.api;
import com.example.agenticorderservice.shared.api.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static com.example.agenticorderservice.shared.api.ApiError.Code.*;
class CustomerApiIT extends PostgreSqlApiTestSupport {
    @Test void executesAllFiveEndpointsAgainstCommittedPostgreSqlState() throws Exception {
        Object value=UUID.randomUUID()+"+tag@example.com";
        var created=request("POST","/customers",mapper.writeValueAsString(Map.of("name","  Älice😀  ","email",value)));
        assertThat(created.statusCode()).isEqualTo(201);var id=json(created).get("id").asString();
        assertThat(created.headers().firstValue("Location")).contains("/customers/"+id);
        assertCustomer(json(created), UUID.fromString(id), "Älice😀", (String) value, true);
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
        assertCustomer(json(updated), UUID.fromString(id), "Changed", (String) changed, false);
        assertThat(jdbc.queryForObject("select email from customers where id=?", String.class, UUID.fromString(id))).isEqualTo(changed);
        assertThat(jdbc.queryForObject("select id from customers where id=?", UUID.class, UUID.fromString(id))).isEqualTo(UUID.fromString(id));
        assertThat(jdbc.queryForObject("select active from customers where id=?", Boolean.class, UUID.fromString(id))).isFalse();
        assertThat(jdbc.queryForObject("select name from customers where id=?",String.class,UUID.fromString(id))).isEqualTo("Changed");
        assertThat(jdbc.queryForObject("select version from customers where id=?",Long.class,UUID.fromString(id))).isEqualTo(version+1);
        stats.clear();var found=request("GET","/customers/"+id,null);assertThat(found.statusCode()).isEqualTo(200);assertThat(found.body()).isEqualTo(updated.body());
        assertThat(stats.getPrepareStatementCount()).isEqualTo(1); // Full materialization: serialization adds no query.
        assertCustomer(json(found), UUID.fromString(id), "Changed", (String) changed, false);
        var listed=request("GET","/customers",null);assertThat(listed.statusCode()).isEqualTo(200);
        assertThat(json(listed)).anySatisfy(row -> { assertThat(row.size()).isEqualTo(4); });
        assertCustomer(member(json(listed), UUID.fromString(id)), UUID.fromString(id), "Changed", (String) changed, false);
        assertError(request("PUT","/customers/"+id,"{\"name\":\"Unwanted\",\"email\":null}"),REQUIRED_FIELD_MISSING);
        assertThat(request("GET","/customers/"+id,null).body()).isEqualTo(updated.body());
        assertError(request("GET","/customers/"+UUID.randomUUID(),null),CUSTOMER_NOT_FOUND);
    }

    @Test void enforcesInactiveUniquenessAtActualTransactionCompletionAndAllowsSameOwner() throws Exception {
        String email=UUID.randomUUID()+"@example.com";
        var created=request("POST","/customers",mapper.writeValueAsString(Map.of("name","Owner","email",email)));
        String id=json(created).get("id").asString();request("PATCH","/customers/"+id,"{\"active\":false}");
        assertThat(jdbc.queryForObject("select active from customers where id=?", Boolean.class, UUID.fromString(id))).isFalse();
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

    @Test
    void listsExactActiveAndInactiveFixturesRegardlessOfUnrelatedRows() throws Exception {
        String activeEmail = "active-" + UUID.randomUUID() + "+tag@example.com";
        String inactiveEmail = "inactive-" + UUID.randomUUID() + "@example.com";
        UUID activeId = persistCustomer("Active fixture", activeEmail, true);
        UUID inactiveId = persistCustomer("Inactive fixture", inactiveEmail, false);
        persistCustomer("Unrelated fixture", UUID.randomUUID() + "@example.com", true);
        assertThat(activeId).isNotEqualTo(inactiveId);
        var stats = factory.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        stats.clear();
        var response = request("GET", "/customers", null);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(stats.getPrepareStatementCount()).isEqualTo(1);
        assertCustomer(member(json(response), activeId), activeId, "Active fixture", activeEmail, true);
        assertCustomer(member(json(response), inactiveId), inactiveId, "Inactive fixture", inactiveEmail, false);
    }

    private UUID persistCustomer(String name, String email, boolean active) {
        return com.example.agenticorderservice.PostgreSqlTestConfiguration.inTransaction(factory, em -> {
            var entity = new com.example.agenticorderservice.customer.domain.Customer(name,
                    new com.example.agenticorderservice.customer.domain.EmailAddress(email));
            if (!active) { entity.deactivate(); }
            em.persist(entity);
            return entity.getId();
        });
    }

    private tools.jackson.databind.JsonNode member(tools.jackson.databind.JsonNode array, UUID id) {
        var matches = new java.util.ArrayList<tools.jackson.databind.JsonNode>();
        array.forEach(row -> { if (id.toString().equals(row.get("id").asString())) { matches.add(row); } });
        assertThat(matches).as("Exactly one response row for fixture %s", id).hasSize(1);
        return matches.getFirst();
    }

    private void assertCustomer(tools.jackson.databind.JsonNode row, UUID id, String name, String value, boolean active) {
        assertThat(row.propertyNames()).containsExactlyInAnyOrder("id", "name", "email", "active");
        assertThat(row.get("id").asString()).isEqualTo(id.toString());
        assertThat(row.get("name").asString()).isEqualTo(name);
        assertThat(row.get("email").asString()).isEqualTo(value);
        assertThat(row.get("active").asBoolean()).isEqualTo(active);
    }
}
