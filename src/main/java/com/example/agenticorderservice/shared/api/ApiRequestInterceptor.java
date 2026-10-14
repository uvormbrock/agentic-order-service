package com.example.agenticorderservice.shared.api;
import jakarta.servlet.http.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import static com.example.agenticorderservice.shared.api.ApiError.Code.*;

public final class ApiRequestInterceptor implements HandlerInterceptor {
    @Override public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method) || !method.getBeanType().getPackageName().matches(".*\\.(product|customer)\\.api")) { return true; }
        String verb = request.getMethod();
        if (verb.equals("POST") || verb.equals("PUT") || verb.equals("PATCH")) {
            String value = request.getContentType();
            // An absent, untyped body is still a missing required body, not unsupported content.
            if (value != null) {
                try {
                    MediaType media = MediaType.parseMediaType(value);
                    if (!media.getType().equalsIgnoreCase("application") || !media.getSubtype().equalsIgnoreCase("json")
                            || (media.getCharset() != null && !media.getCharset().equals(StandardCharsets.UTF_8))) {
                        throw new ApiInputException(UNSUPPORTED_MEDIA_TYPE);
                    }
                } catch (IllegalArgumentException invalid) { throw new ApiInputException(UNSUPPORTED_MEDIA_TYPE); }
            } else if (request.getContentLengthLong() > 0 || request.getHeader("Transfer-Encoding") != null) {
                throw new ApiInputException(UNSUPPORTED_MEDIA_TYPE);
            }
        }
        String accept = request.getHeader(HttpHeaders.ACCEPT);
        if (accept != null) {
            try {
                var matches = MediaType.parseMediaTypes(accept).stream()
                        .filter(media -> media.isCompatibleWith(MediaType.APPLICATION_JSON))
                        .sorted(java.util.Comparator.comparingInt((MediaType media) -> media.isWildcardType() ? 0 : media.isWildcardSubtype() ? 1 : 2).reversed())
                        .toList();
                boolean compatible = !matches.isEmpty() && matches.getFirst().getQualityValue() > 0;
                if (!compatible) { throw new ApiInputException(NOT_ACCEPTABLE); }
            } catch (IllegalArgumentException invalid) { throw new ApiInputException(NOT_ACCEPTABLE); }
        }
        Object variables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (variables instanceof Map<?, ?> paths && paths.get("id") instanceof String id
                && !id.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
            throw new ApiInputException(INVALID_RESOURCE_ID);
        }
        return true;
    }
}
