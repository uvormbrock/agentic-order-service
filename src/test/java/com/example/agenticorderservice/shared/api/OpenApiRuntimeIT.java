package com.example.agenticorderservice.shared.api;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"integration-test", "demo"})
class OpenApiRuntimeIT extends PostgreSqlApiTestSupport {
    @Test
    void runtimeJsonYamlSnapshotAndSwaggerUiAgree() throws Exception {
        var json = request("GET", "/v3/api-docs", null);
        var yaml = request("GET", "/v3/api-docs.yaml", null);
        assertThat(json.statusCode()).isEqualTo(200);
        assertThat(yaml.statusCode()).isEqualTo(200);
        assertThat(Yaml.mapper().readTree(yaml.body())).isEqualTo(Json.mapper().readTree(json.body()));
        var snapshot = Path.of("docs/02-architecture/openapi.yaml");
        if (Boolean.getBoolean("ch001.generateOpenApi")) Files.writeString(snapshot, yaml.body());
        assertThat(Yaml.mapper().readTree(Files.readString(snapshot)))
                .isEqualTo(Json.mapper().readTree(json.body()));
        assertThat(request("GET", "/swagger-ui/index.html", null).statusCode()).isEqualTo(200);
        assertThat(Json.mapper()
                        .readTree(request("GET", "/v3/api-docs/swagger-config", null)
                                .body())
                        .path("url")
                        .asText())
                .isEqualTo("/v3/api-docs");
    }
}
