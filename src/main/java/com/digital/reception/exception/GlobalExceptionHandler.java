package com.digital.reception.exception;

import com.digital.reception.dto.DteResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
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
    public ResponseEntity<DteResponse> handleInvalidPayload(Exception exception) {
        DteResponse response = new DteResponse(2, "00", 2, "RECHAZADO", null, null,
                LocalDateTime.now().format(PROCESSING_TIME), null, "ERROR_CODIGO",
                "RECHAZADO POR REGLAS DE ESTRUCTURA Y SCHEMA", List.of(exception.getMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
