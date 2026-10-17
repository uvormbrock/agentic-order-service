package com.example.agenticorderservice.shared.api;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.web.bind.annotation.*;

@RestController
public final class ApiErrorController implements ErrorController {
    private final ApiErrorResponseWriter writer;

    public ApiErrorController(ApiErrorResponseWriter writer) {
        this.writer = writer;
    }
    // Boot property placeholders are not URI variables. ERROR dispatch retains the original method;
    // this read-only renderer intentionally accepts every method, as Boot's error controller does.
    @RequestMapping("${spring.web.error.path:${error.path:/error}}")
    public void error(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        ApiError.Code code = status instanceof Integer number
                ? switch (number) {
                    case 404 -> ApiError.Code.ENDPOINT_NOT_FOUND;
                    case 405 -> ApiError.Code.METHOD_NOT_ALLOWED;
                    case 406 -> ApiError.Code.NOT_ACCEPTABLE;
                    case 415 -> ApiError.Code.UNSUPPORTED_MEDIA_TYPE;
                    default -> ApiError.Code.INTERNAL_SERVER_ERROR;
                }
                : ApiError.Code.INTERNAL_SERVER_ERROR;
        HttpFailureDiagnostic.record(
                request,
                code,
                request.getAttribute(RequestDispatcher.ERROR_EXCEPTION) instanceof Throwable failure ? failure : null);
        writer.write(response, code);
    }
}
