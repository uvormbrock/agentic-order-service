package com.example.agenticorderservice.shared.api;

import static com.example.agenticorderservice.shared.api.ApiError.Code.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

public final class ApiRequestInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!isFeatureHandler(handler)) {
            return true;
        }
        if (handler instanceof HandlerMethod method
                && method.getBeanType() == com.example.agenticorderservice.product.api.ProductImageController.class) {
            validateImageHeaders(request);
        } else {
            validateContentType(request);
            validateAccept(request.getHeader(HttpHeaders.ACCEPT));
        }
        validatePath(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE));
        return true;
    }

    private void validateImageHeaders(HttpServletRequest request) {
        if (request.getMethod().equals("PUT")) {
            validateMultipartType(request.getContentType());
        }
        if (request.getMethod().equals("GET") || request.getMethod().equals("HEAD")) {
            validateImageAccept(request.getHeader(HttpHeaders.ACCEPT));
        }
    }

    private void validateMultipartType(String contentType) {
        try {
            if (contentType == null
                    || !MediaType.MULTIPART_FORM_DATA.isCompatibleWith(MediaType.parseMediaType(contentType))) {
                throw new ApiInputException(UNSUPPORTED_MEDIA_TYPE);
            }
        } catch (IllegalArgumentException failure) {
            throw new ApiInputException(UNSUPPORTED_MEDIA_TYPE);
        }
    }

    private void validateImageAccept(String accept) {
        if (accept == null) return;
        try {
            var types = MediaType.parseMediaTypes(accept);
            boolean accepted = types.stream()
                    .anyMatch(type -> type.getQualityValue() > 0
                            && (type.isCompatibleWith(MediaType.IMAGE_JPEG)
                                    || type.isCompatibleWith(MediaType.IMAGE_PNG)));
            if (!accepted) throw new ApiInputException(NOT_ACCEPTABLE);
        } catch (IllegalArgumentException failure) {
            throw new ApiInputException(NOT_ACCEPTABLE);
        }
    }

    private boolean isFeatureHandler(Object handler) {
        return handler instanceof HandlerMethod method
                && method.getBeanType().getPackageName().matches(".*\\.(product|customer|order)\\.api");
    }

    private void validateContentType(HttpServletRequest request) {
        String method = request.getMethod();
        if (!method.equals("POST") && !method.equals("PUT") && !method.equals("PATCH")) {
            return;
        }
        String value = request.getContentType();
        if (value == null) {
            // Empty untyped input is a missing body; actual untyped bodies are unsupported.
            if (request.getContentLengthLong() > 0 || request.getHeader("Transfer-Encoding") != null) {
                throw new ApiInputException(UNSUPPORTED_MEDIA_TYPE);
            }
            return;
        }
        try {
            MediaType media = MediaType.parseMediaType(value);
            if (!isUtf8Json(media)) {
                throw new ApiInputException(UNSUPPORTED_MEDIA_TYPE);
            }
        } catch (IllegalArgumentException invalid) {
            throw new ApiInputException(UNSUPPORTED_MEDIA_TYPE);
        }
    }

    private boolean isUtf8Json(MediaType media) {
        return media.getType().equalsIgnoreCase("application")
                && media.getSubtype().equalsIgnoreCase("json")
                && (media.getCharset() == null || media.getCharset().equals(StandardCharsets.UTF_8));
    }

    private void validateAccept(String accept) {
        if (accept == null) {
            return;
        }
        try {
            var matches = MediaType.parseMediaTypes(accept).stream()
                    .filter(media -> media.isCompatibleWith(MediaType.APPLICATION_JSON))
                    .sorted(Comparator.comparingInt(this::specificity).reversed())
                    .toList();
            if (matches.isEmpty() || matches.getFirst().getQualityValue() <= 0) {
                throw new ApiInputException(NOT_ACCEPTABLE);
            }
        } catch (IllegalArgumentException invalid) {
            throw new ApiInputException(NOT_ACCEPTABLE);
        }
    }

    private int specificity(MediaType media) {
        if (media.isWildcardType()) {
            return 0;
        }
        if (media.isWildcardSubtype()) {
            return 1;
        }
        return 2;
    }

    private void validatePath(Object variables) {
        if (variables instanceof Map<?, ?> paths
                && paths.get("id") instanceof String id
                && !id.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
            throw new ApiInputException(INVALID_RESOURCE_ID);
        }
    }
}
