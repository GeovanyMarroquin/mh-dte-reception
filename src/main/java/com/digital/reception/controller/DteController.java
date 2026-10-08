package com.digital.reception.controller;

import com.digital.reception.dto.*;
import com.digital.reception.service.DteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class DteController {
    private final DteService dteService;

    @PostMapping(path = "/seguridad/auth", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public AuthResponse authenticate(
            @RequestParam String user,
            @RequestParam String pwd
    ) {
        if (user.isBlank() || pwd.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "user y pwd son obligatorios");
        }
        return new AuthResponse("OK", new AuthResponse.Body(
                "mock-token-eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkiLCJuYW1lIjoiTW9jayBVc2VyIiwiaWF0IjoxNTE2MjM5MDIyfQ",
                List.of("ROLE_USER"), "Bearer"));
    }

    @PostMapping(
            path = "/fesv/recepciondte",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<DteResponse> receive(
            @RequestHeader("Authorization") String authorization,
            @RequestBody ReceptionRequest request
    ) throws Exception {
        log.info("Endpoint 1 {}", "/fesv/recepciondte");
        requireBearerToken(authorization);
        DteResponse response = dteService.receive(request);
        return ResponseEntity.status("PROCESADO".equals(response.estado()) ? 200 : 400).body(response);
    }

    @PostMapping(
            path = "/fesv/recepcion/consultadte",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<DteResponse> consult(
            @RequestHeader("Authorization") String authorization,
            @RequestHeader("User-Agent") String userAgent,
            @RequestBody ConsultaDteRequest request
    ) {
        log.info("Endpoint 2 {}", "/fesv/recepcion/consultadte");
        requireBearerToken(authorization);
        if (userAgent.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User-Agent es obligatorio");
        }
        ConsultDteResult result = dteService.consult(request);
        return ResponseEntity.status(result.httpStatus()).body(result.response());
    }

    private void requireBearerToken(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)
                || authorization.substring(7).isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Se requiere Authorization: Bearer <token>");
        }
    }
}
