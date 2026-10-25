package com.digital.reception.service;

import com.digital.reception.dto.AnulacionRequest;
import com.digital.reception.dto.AnulacionResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DteAnnulServiceTest {
    private static final String EVENTO_CODIGO = "CF89B21E-4268-4711-832D-8A54866B1763";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private DteSchemaValidatorService schemaValidator;

    @Mock
    private com.digital.reception.repository.DteRepository repository;

    @InjectMocks
    private DteService service;

    @Test
    void annulReturnsProcessedResponseOnValidEvent() {
        String payload = validEventPayload();
        AnulacionRequest request = new AnulacionRequest("00", 1L, 3, toJws(payload));
        when(schemaValidator.validate(any(JsonNode.class), eq(null), eq("invalidacion")))
                .thenReturn(List.of());

        AnulacionResponse response = service.annul(request);

        assertEquals(2, response.version());
        assertEquals("00", response.ambiente());
        assertEquals(1, response.versionApp());
        assertEquals("PROCESADO", response.estado());
        assertEquals(EVENTO_CODIGO, response.codigoGeneracion());
        assertNotNull(response.selloRecibido());
        assertEquals(40, response.selloRecibido().length());
        assertNotNull(response.fhProcesamiento());
        assertEquals("001", response.codigoMsg());
        assertEquals("Invalidación Recibida y Procesada", response.descripcionMsg());
        assertNull(response.observaciones());
    }

    @Test
    void annulReturnsRejectedResponseOnSchemaObservations() {
        String payload = validEventPayload();
        AnulacionRequest request = new AnulacionRequest("00", 1L, 3, toJws(payload));
        when(schemaValidator.validate(any(JsonNode.class), eq(null), eq("invalidacion")))
                .thenReturn(List.of("$.motivo: required property not found"));

        AnulacionResponse response = service.annul(request);

        assertEquals("RECHAZADO", response.estado());
        assertEquals(EVENTO_CODIGO, response.codigoGeneracion());
        assertNull(response.selloRecibido());
        assertNull(response.fhProcesamiento());
        assertEquals("ERROR_CODIGO", response.codigoMsg());
        assertEquals(List.of("$.motivo: required property not found"), response.observaciones());
    }

    @Test
    void annulReturnsRejectedResponseOnMalformedJws() {
        AnulacionRequest request = new AnulacionRequest("00", 1L, 3, "no-es-un-jws");

        AnulacionResponse response = service.annul(request);

        assertEquals("RECHAZADO", response.estado());
        assertNull(response.codigoGeneracion());
        assertNull(response.selloRecibido());
        assertNull(response.fhProcesamiento());
        assertEquals("ERROR_CODIGO", response.codigoMsg());
        assertEquals(1, response.observaciones().size());
    }

    @Test
    void craftedPayloadPassesRealInvalidacionSchema() throws Exception {
        DteSchemaValidatorService realValidator = new DteSchemaValidatorService();
        realValidator.loadSchemas();
        JsonNode evento = MAPPER.readTree(validEventPayload());

        List<String> observations = realValidator.validate(evento, null, "invalidacion");

        assertEquals(List.of(), observations);
    }

    private static String toJws(String payload) {
        String encoded = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return "e30." + encoded + ".sig";
    }

    private static String validEventPayload() {
        return """
                {
                  "identificacion": {
                    "version": 3,
                    "ambiente": "00",
                    "codigoGeneracion": "%s",
                    "fecEmi": "2026-10-25",
                    "horEmi": "09:50:52",
                    "fusion": "06142803901121"
                  },
                  "emisor": {
                    "nit": "06142803901121",
                    "nombre": "EMISOR SA DE CV",
                    "codEstableMH": "M001",
                    "codEstable": null,
                    "codPuntoVentaMH": "P001",
                    "codPuntoVenta": null,
                    "telefono": "22222222",
                    "correo": "emisor@test.com"
                  },
                  "documento": {
                    "tipoDte": "01",
                    "codigoGeneracion": "CA7048BB-706D-46D1-99DF-ECEB4CA724DE",
                    "selloRecibido": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                    "numeroControl": "DTE-01-M001P001-000000000000001",
                    "fecEmi": "2026-10-24",
                    "codigoGeneracionR": null,
                    "tipoDocumento": null,
                    "numDocumento": null,
                    "nombre": null,
                    "telefono": null,
                    "correo": null
                  },
                  "motivo": {
                    "tipoAnulacion": 2,
                    "motivoAnulacion": null,
                    "nombreResponsable": "JUAN PEREZ",
                    "tipDocResponsable": "36",
                    "numDocResponsable": "000000000",
                    "nombreSolicita": "JUAN PEREZ",
                    "tipDocSolicita": "36",
                    "numDocSolicita": "000000000"
                  }
                }
                """
                .formatted(EVENTO_CODIGO);
    }
}
