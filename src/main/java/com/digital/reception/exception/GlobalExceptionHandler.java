package com.digital.reception.exception;

import com.digital.reception.dto.AnulacionResponse;
import com.digital.reception.dto.DteResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final DateTimeFormatter PROCESSING_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @ExceptionHandler({IllegalArgumentException.class, JsonProcessingException.class,
            HttpMessageNotReadableException.class})
    public ResponseEntity<?> handleInvalidPayload(Exception exception, HttpServletRequest request) {
        String path = request == null ? "" : request.getRequestURI();
        if (path != null && path.contains("/fesv/anulardte")) {
            AnulacionResponse response = new AnulacionResponse(2, "00", 1, "RECHAZADO", null, null, null,
                    "ERROR_CODIGO", "RECHAZADO POR REGLAS DE ESTRUCTURA Y SCHEMA",
                    List.of(exception.getMessage()));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        DteResponse response = new DteResponse(2, "00", 2, "RECHAZADO", null, null,
                LocalDateTime.now().format(PROCESSING_TIME), null, "ERROR_CODIGO",
                "RECHAZADO POR REGLAS DE ESTRUCTURA Y SCHEMA", List.of(exception.getMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
