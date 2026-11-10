package com.plass.common;

import java.util.LinkedHashMap;
import java.util.Map;

import com.plass.booking.SeatsUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns exceptions into RFC 9457 "problem detail" responses, e.g.
 * {@code {"status":409,"title":"Conflict","detail":"...","code":"SEATS_UNAVAILABLE"}}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> handleApiException(ApiException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        problem.setTitle(ex.getStatus().getReasonPhrase());
        problem.setProperty("code", ex.getCode());
        if (ex instanceof SeatsUnavailableException seatsEx) {
            problem.setProperty("seats", seatsEx.getSeatLabels());
        }
        return ResponseEntity.status(ex.getStatus()).body(problem);
    }

    /** Last line of defence when two transactions update the same row at the same time. */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ProblemDetail> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        log.info("Optimistic lock conflict: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "This record was changed by another request. Please refresh and try again.");
        problem.setTitle(HttpStatus.CONFLICT.getReasonPhrase());
        problem.setProperty("code", "CONCURRENT_UPDATE");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        // The constraint that failed per field (e.g. "NotBlank", "Size"), so clients can show the
        // error in their own language instead of the English default message.
        Map<String, String> errorCodes = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            if (errors.putIfAbsent(error.getField(), error.getDefaultMessage()) == null) {
                errorCodes.put(error.getField(), error.getCode());
            }
        });

        ProblemDetail problem = ex.getBody();
        problem.setDetail("Some fields are invalid.");
        problem.setProperty("code", "VALIDATION_FAILED");
        problem.setProperty("errors", errors);
        problem.setProperty("errorCodes", errorCodes);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }
}
