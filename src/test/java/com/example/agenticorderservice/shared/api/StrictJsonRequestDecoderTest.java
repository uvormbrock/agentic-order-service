package com.example.agenticorderservice.shared.api;
import com.example.agenticorderservice.product.api.*;
import com.example.agenticorderservice.customer.api.*;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static org.assertj.core.api.Assertions.*;
import static com.example.agenticorderservice.shared.api.ApiError.Code.*;

class StrictJsonRequestDecoderTest {
    private final StrictJsonRequestDecoder decoder = new StrictJsonRequestDecoder();
    static Stream<Arguments> invalid() {
        return Stream.of(
            Arguments.of("",REQUIRED_FIELD_MISSING),Arguments.of("{}",REQUIRED_FIELD_MISSING),
            Arguments.of("{\"version\":0,\"extra\":true}",UNKNOWN_FIELD),
            Arguments.of("{\"extra\":true,\"version\":0}",UNKNOWN_FIELD),
            Arguments.of("{\"id\":null,\"zzz\":true}",UNSUPPORTED_FIELD),
            Arguments.of("{\"zzz\":true,\"id\":null}",UNSUPPORTED_FIELD),
            Arguments.of("   ",MALFORMED_JSON),Arguments.of("null",MALFORMED_JSON),Arguments.of("[]",MALFORMED_JSON),
            Arguments.of("{",MALFORMED_JSON),Arguments.of("{\"name\":\"A\",\"name\":\"B\",\"price\":1}",MALFORMED_JSON),
            Arguments.of("{\"extra\":{\"x\":1,\"x\":2}}",MALFORMED_JSON),
            Arguments.of("{} {}",MALFORMED_JSON),Arguments.of("{} true",MALFORMED_JSON),Arguments.of("{} x",MALFORMED_JSON),
            Arguments.of("{\"extra\":1,\"name\":3,\"price\":null}",UNKNOWN_FIELD),
            Arguments.of("{\"name\":3,\"extra\":1,\"price\":null}",UNKNOWN_FIELD),
            Arguments.of("{\"version\":0,\"name\":3}",UNSUPPORTED_FIELD),
            Arguments.of("{\"id\":null}",UNSUPPORTED_FIELD),Arguments.of("{\"active\":false}",UNSUPPORTED_FIELD),
            Arguments.of("{\"name\":3,\"price\":null}",INVALID_FIELD_TYPE),
            Arguments.of("{\"price\":\"1\"}",INVALID_FIELD_TYPE),Arguments.of("{\"name\":true,\"price\":1}",INVALID_FIELD_TYPE),
            Arguments.of("{\"name\":{},\"price\":1}",INVALID_FIELD_TYPE),Arguments.of("{\"name\":\"\",\"price\":null}",REQUIRED_FIELD_MISSING),
            Arguments.of("{\"name\":\"\",\"price\":-1}",INVALID_NAME),
            Arguments.of("{\"price\":-1,\"name\":\"\"}",INVALID_NAME),
            Arguments.of("{\"name\":null,\"price\":1}",REQUIRED_FIELD_MISSING),
            Arguments.of("{\"name\":\"A\",\"price\":1.000}",INVALID_PRODUCT_PRICE),
            Arguments.of("{\"name\":\"A\",\"price\":1e0}",INVALID_PRODUCT_PRICE),
            Arguments.of("{\"name\":false,\"extra\":true",MALFORMED_JSON)
        );
    }
    @ParameterizedTest @MethodSource("invalid") void deterministicCategories(String input,ApiError.Code expected) {
        try { decoder.decode(input.getBytes(StandardCharsets.UTF_8),ProductDetailsRequest.class); fail("Expected rejection"); }
        catch(RuntimeException failure) { assertThat(ApiExceptionHandler.classify(failure)).isEqualTo(expected); }
    }
    @ParameterizedTest @CsvSource({"{},REQUIRED_FIELD_MISSING","{active:null},REQUIRED_FIELD_MISSING","{active:true},UNSUPPORTED_FIELD","{active:1},INVALID_FIELD_TYPE","{active:foo},INVALID_FIELD_TYPE","{name:x},UNSUPPORTED_FIELD","{extra:x},UNKNOWN_FIELD"})
    void patchPolicies(String shorthand,ApiError.Code expected) {
        String body=shorthand.replace("active","\"active\"").replace("null","null").replace("foo","\"foo\"")
                .replace("name","\"name\"").replace("extra","\"extra\"").replace(":x",":\"x\"");
        assertThatThrownBy(() -> decoder.decode(body.getBytes(StandardCharsets.UTF_8),DeactivateProductRequest.class))
                .isInstanceOf(ApiInputException.class).satisfies(e -> assertThat(ApiExceptionHandler.classify(e)).isEqualTo(expected));
    }
    @Test void acceptsTrailingJsonWhitespaceAndInactivePatch() {
        assertThat(decoder.decode("{\"active\":false} \t\r\n".getBytes(StandardCharsets.UTF_8),DeactivateCustomerRequest.class)).isEqualTo(new DeactivateCustomerRequest(false));
    }
    @Test void rejectsInvalidUtf8AndInvalidSurrogates() {
        assertThatThrownBy(() -> decoder.decode(new byte[]{(byte)0xff},ProductDetailsRequest.class)).satisfies(e -> assertThat(ApiExceptionHandler.classify(e)).isEqualTo(MALFORMED_JSON));
        assertThatThrownBy(() -> decoder.decode("{\"name\":\"\\uD800\",\"price\":1}".getBytes(StandardCharsets.UTF_8),ProductDetailsRequest.class)).satisfies(e -> assertThat(ApiExceptionHandler.classify(e)).isEqualTo(INVALID_NAME));
    }
    @Test void verifiesUnicodeAndEmailLengthBoundaries() {
        var mapper=tools.jackson.databind.json.JsonMapper.builder().build();
        for(int count:new int[]{1,200,201}) {
            String name="😀".repeat(count);byte[] body=mapper.writeValueAsBytes(java.util.Map.of("name",name,"price",1));
            if(count<=200) { assertThat(((ProductDetailsRequest)decoder.decode(body,ProductDetailsRequest.class)).name()).isEqualTo(name); }
            else { assertThatThrownBy(() -> decoder.decode(body,ProductDetailsRequest.class)).satisfies(e -> assertThat(ApiExceptionHandler.classify(e)).isEqualTo(INVALID_NAME)); }
        }
        for(int count:new int[]{254,255}) {
            String email="a".repeat(count-12)+"@example.com";byte[] body=mapper.writeValueAsBytes(java.util.Map.of("name","Valid","email",email));
            if(count==254) { assertThat(((CustomerDetailsRequest)decoder.decode(body,CustomerDetailsRequest.class)).email()).isEqualTo(email); }
            else { assertThatThrownBy(() -> decoder.decode(body,CustomerDetailsRequest.class)).satisfies(e -> assertThat(ApiExceptionHandler.classify(e)).isEqualTo(INVALID_EMAIL)); }
        }
    }
}
