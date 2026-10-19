package com.example.agenticorderservice.product.api;

import static com.example.agenticorderservice.product.image.ProductImageFixtures.*;
import static org.assertj.core.api.Assertions.*;

import com.example.agenticorderservice.shared.api.PostgreSqlApiTestSupport;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "app.product-images.writes-enabled=true")
class ProductImageHttpIT extends PostgreSqlApiTestSupport {
    static final Path ROOT = root();

    static Path root() {
        try {
            return Files.createTempDirectory("CH-002-image-http-").toRealPath();
        } catch (Exception failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.product-images.storage-directory", ROOT::toString);
    }

    @Test
    void actualMultipartUploadAndBinaryReadsPreserveBytesAndInactiveStatus() throws Exception {
        UUID id = product();
        byte[] original = image("jpg", 960, 480);
        assertThat(upload(id, original).statusCode()).isEqualTo(204);
        var read = getBytes(id, "");
        assertThat(read.statusCode()).isEqualTo(200);
        assertThat(read.body()).containsExactly(original);
        assertThat(read.headers().firstValue("Content-Type")).contains("image/jpeg");
        var thumbnail = getBytes(id, "/thumbnail");
        assertThat(thumbnail.statusCode()).isEqualTo(200);
        var decoded = ImageIO.read(new ByteArrayInputStream(thumbnail.body()));
        assertThat(decoded.getWidth()).isEqualTo(320);
        assertThat(decoded.getHeight()).isEqualTo(160);
        assertThat(jdbc.queryForObject("select active from products where id=?", Boolean.class, id))
                .isFalse();
        assertThat(request("DELETE", "/products/" + id + "/image", null).statusCode())
                .isEqualTo(204);
        assertThat(getBytes(id, "").statusCode()).isEqualTo(404);
    }

    @Test
    void realDemoSeedHelperIsRepeatableForEightMatchingAssetsAndLeavesImagelessProductValid() throws Exception {
        Path state = Files.createTempDirectory("CH-002-seed-state-");
        Path script = Files.createTempFile("CH-002-seed-", ".py");
        String runner = """
                import importlib.util,json,pathlib,sys
                spec=importlib.util.spec_from_file_location('demo_seed', 'demo/demo.py')
                demo=importlib.util.module_from_spec(spec);spec.loader.exec_module(demo)
                demo.BASE=sys.argv[1];demo.STATE=pathlib.Path(sys.argv[2])
                demo.database=lambda config:None
                demo.app=lambda:None
                demo.seed_images({})
                demo.seed_images({})
                """;
        Files.writeString(script, runner);
        var records = mapper.createArrayNode();
        java.util.Map<String, UUID> ids = new java.util.LinkedHashMap<>();
        for (String name : new String[] {
            "Demo Keyboard v2",
            "Demo Mouse",
            "Demo Notebook",
            "Demo Monitor",
            "Demo Sticker",
            "Demo Desk",
            "Demo Pen",
            "Demo Legacy Item"
        }) {
            UUID id = UUID.randomUUID();
            jdbc.update(
                    "insert into products(id,name,price,active,version) values (?,?,25,?,0)",
                    id,
                    name,
                    !name.equals("Demo Legacy Item"));
            ids.put(name, id);
            records.addObject().put("id", id.toString());
        }
        UUID imageless = product();
        Files.writeString(
                state.resolve("seed.json"),
                mapper.writeValueAsString(mapper.createObjectNode().set("products", records)));
        Path log = state.resolve("seed.log");
        var process = new ProcessBuilder(
                        "python3", "-B", script.toString(), "http://127.0.0.1:" + port, state.toString())
                .redirectErrorStream(true)
                .redirectOutput(log.toFile())
                .start();
        assertThat(process.waitFor(45, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        assertThat(process.exitValue()).withFailMessage(Files.readString(log)).isZero();
        for (UUID id : ids.values()) {
            assertThat(jdbc.queryForObject("select count(*) from product_images where product_id=?", Integer.class, id))
                    .isEqualTo(1);
            assertThat(ImageIO.read(new ByteArrayInputStream(getBytes(id, "").body())))
                    .isNotNull();
            assertThat(ImageIO.read(
                            new ByteArrayInputStream(getBytes(id, "/thumbnail").body())))
                    .isNotNull();
        }
        assertThat(jdbc.queryForObject(
                        "select active from products where id=?", Boolean.class, ids.get("Demo Legacy Item")))
                .isFalse();
        assertThat(getBytes(imageless, "").statusCode()).isEqualTo(404);
        assertThat(request("GET", "/products/" + imageless, null).statusCode()).isEqualTo(200);
    }

    @Test
    void containerRejectsOversizedMultipartAndRetainsPriorAssociationAndBytes() throws Exception {
        UUID id = product();
        byte[] original = image("jpg", 10, 10);
        assertThat(upload(id, original).statusCode()).isEqualTo(204);
        var rejected = upload(id, new byte[10 * 1024 * 1024 + 1]);
        assertThat(rejected.statusCode()).isEqualTo(413);
        assertThat(rejected.body()).contains("PRODUCT_IMAGE_TOO_LARGE");
        assertThat(getBytes(id, "").body()).containsExactly(original);
    }

    UUID product() {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into products(id,name,price,active,version) values (?,'HTTP Image',25,false,0)", id);
        return id;
    }

    HttpResponse<String> upload(UUID id, byte[] bytes) throws Exception {
        String boundary = "image-test-" + UUID.randomUUID();
        var body = new ByteArrayOutputStream();
        body.write(("--" + boundary
                        + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"image.jpg\"\r\nContent-Type: image/jpeg\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8));
        body.write(bytes);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return client.send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/products/" + id + "/image"))
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .PUT(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    HttpResponse<byte[]> getBytes(UUID id, String suffix) throws Exception {
        return client.send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/products/" + id + "/image" + suffix))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofByteArray());
    }
}
