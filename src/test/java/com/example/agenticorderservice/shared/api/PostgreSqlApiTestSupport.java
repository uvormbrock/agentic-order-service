package com.example.agenticorderservice.shared.api;
import com.example.agenticorderservice.PostgreSqlTestConfiguration;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.JsonNode;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties="spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("integration-test")
@Import(PostgreSqlTestConfiguration.class)
public abstract class PostgreSqlApiTestSupport {
    @LocalServerPort protected int port;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected JsonMapper mapper;
    @Autowired protected jakarta.persistence.EntityManagerFactory factory;
    protected HttpClient client;
    @BeforeEach void client() { client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(); }
    protected HttpResponse<String> request(String verb,String path,String body) throws Exception {
        var builder=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(20)).header("Content-Type","application/json");
        return client.send(builder.method(verb,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
    }
    protected JsonNode json(HttpResponse<String> response) { return mapper.readTree(response.body()); }
    protected void assertError(HttpResponse<String> response,ApiError.Code code) {
        assertThat(response.statusCode()).isEqualTo(code.status());assertThat(response.body()).isEqualTo(mapper.writeValueAsString(new ApiError(code)));
    }
}
