package com.example.agenticorderservice.shared.api;
public final class ApiInputException extends RuntimeException {
    private final ApiError.Code code;
    public ApiInputException(ApiError.Code code) { super(code.name()); this.code = code; }
    public ApiError.Code code() { return code; }
}
