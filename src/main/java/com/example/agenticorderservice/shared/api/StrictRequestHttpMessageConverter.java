package com.example.agenticorderservice.shared.api;

import java.io.IOException;
import org.springframework.http.*;
import org.springframework.http.converter.AbstractHttpMessageConverter;

public final class StrictRequestHttpMessageConverter extends AbstractHttpMessageConverter<Object> {
    private final StrictJsonRequestDecoder decoder;

    public StrictRequestHttpMessageConverter(StrictJsonRequestDecoder decoder) {
        super(MediaType.APPLICATION_JSON);
        this.decoder = decoder;
    }

    @Override
    protected boolean supports(Class<?> type) {
        return decoder.supports(type);
    }

    @Override
    public boolean canRead(Class<?> type, MediaType mediaType) {
        // MVC uses octet-stream internally for absent Content-Type. The interceptor rejects
        // actual untyped/explicit octet-stream bodies; empty input must reach MissingBodyAdvice.
        return supports(type)
                && (mediaType == null
                        || MediaType.APPLICATION_JSON.isCompatibleWith(mediaType)
                        || MediaType.APPLICATION_OCTET_STREAM.equals(mediaType));
    }

    @Override
    public boolean canWrite(Class<?> type, MediaType mediaType) {
        return false;
    }

    @Override
    protected Object readInternal(Class<?> type, HttpInputMessage message) throws IOException {
        return decoder.decode(message.getBody().readAllBytes(), type);
    }

    @Override
    protected void writeInternal(Object value, HttpOutputMessage message) {
        throw new UnsupportedOperationException("Input-only converter");
    }
}
