package dev.zeann3th.file.exception;

import io.opentelemetry.api.trace.Span;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class HttpGlobalExceptionHandler {

    @ExceptionHandler(value = CommandException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(CommandException ex) {
        ErrorCode errorCode = ex.getErrorCode();

        ApiResponse<Void> response = new ApiResponse<>(
                errorCode.getErrorType().name(),
                errorCode.name(),
                ex.getMessage(),
                null,
                getCurrentTraceId()
        );

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unhandled exception: ", ex);

        ApiResponse<Void> response = new ApiResponse<>(
                ErrorCode.FS9999.getErrorType().name(),
                ErrorCode.FS9999.name(),
                ErrorCode.FS9999.getMessage(),
                null,
                getCurrentTraceId()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private String getCurrentTraceId() {
        return Span.current().getSpanContext().getTraceId();
    }
}
