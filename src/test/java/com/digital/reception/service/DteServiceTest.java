package com.digital.reception.service;

import com.digital.reception.dto.ConsultaDteRequest;
import com.digital.reception.dto.ConsultDteResult;
import com.digital.reception.dto.DteResponse;
import com.digital.reception.dto.ReceptionRequest;
import com.digital.reception.entity.DteEntity;
import com.digital.reception.repository.DteRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

@ExtendWith(MockitoExtension.class)
class DteServiceTest {
    private static final String GENERATION_CODE = "CA7048BB-706D-46D1-99DF-ECEB4CA724DE";

    @Mock
    private DteSchemaValidatorService schemaValidator;

    @Mock
    private DteRepository repository;

    @InjectMocks
    private DteService service;

    @Test
    void consultReturnsStoredProcessedDocumentByGenerationCode() {
        DteEntity entity = new DteEntity(GENERATION_CODE, "06142803901121", "01", "00",
                "DTE-01-M001P001-000000000000001", "20269E9D4DC0292F4681AD759B0B0F5CA99DC23G",
                "PROCESADO", "10", "001", "RECIBIDO", "04/10/2026 20:16:36", "{}", "[]");
        when(repository.findByCodigoGeneracionAndNitEmisorAndTdte(
                GENERATION_CODE, "06142803901121", "01")).thenReturn(Optional.of(entity));

        ConsultDteResult result = service.consult(
                new ConsultaDteRequest("06142803901121", "01", GENERATION_CODE));

        verify(repository).findByCodigoGeneracionAndNitEmisorAndTdte(GENERATION_CODE, "06142803901121", "01");
        assertEquals(200, result.httpStatus());
        assertEquals("PROCESADO", result.response().estado());
        assertEquals("00", result.response().ambiente());
        assertEquals("20269E9D4DC0292F4681AD759B0B0F5CA99DC23G", result.response().selloRecibido());
        assertEquals("10", result.response().clasificaMsg());
        assertEquals("001", result.response().codigoMsg());
        assertEquals("RECIBIDO", result.response().descripcionMsg());
        assertEquals("04/10/2026 20:16:36", result.response().fhProcesamiento());
        assertEquals(0, result.response().observaciones().size());
    }

    @Test
    void consultReturnsOfficialNotFoundResponse() {
        when(repository.findByCodigoGeneracionAndNitEmisorAndTdte(
                GENERATION_CODE, "06142803901121", "01")).thenReturn(Optional.empty());

        ConsultDteResult result = service.consult(
                new ConsultaDteRequest("06142803901121", "01", GENERATION_CODE));

        assertEquals(400, result.httpStatus());
        assertEquals("RECHAZADO", result.response().estado());
        assertEquals(GENERATION_CODE, result.response().codigoGeneracion());
        assertNull(result.response().selloRecibido());
        assertNull(result.response().clasificaMsg());
        assertEquals("301", result.response().codigoMsg());
        assertEquals("DOCUMENTO NO EXISTE", result.response().descripcionMsg());
        assertNull(result.response().observaciones());
    }

    @Test
    void consultReturnsStoredRejectedDocumentWithObservations() {
        DteEntity entity = new DteEntity(GENERATION_CODE, "06142803901121", "01", "00", null, null,
                "RECHAZADO", null, "ERROR_CODIGO", "RECHAZADO POR REGLAS DE ESTRUCTURA Y SCHEMA",
                "04/10/2026 20:16:36", "{}", "[\"$.receptor: required property not found\"]");
        when(repository.findByCodigoGeneracionAndNitEmisorAndTdte(
                GENERATION_CODE, "06142803901121", "01")).thenReturn(Optional.of(entity));

        ConsultDteResult result = service.consult(
                new ConsultaDteRequest("06142803901121", "01", GENERATION_CODE));

        assertEquals(400, result.httpStatus());
        assertEquals("RECHAZADO", result.response().estado());
        assertNull(result.response().selloRecibido());
        assertEquals("ERROR_CODIGO", result.response().codigoMsg());
        assertNull(result.response().observaciones());
    }

    @Test
    void receivePersistsRejectedDteMetadataAndNormalizedIssuerNit() throws Exception {
        String payload = "{\"identificacion\":{\"codigoGeneracion\":\"" + GENERATION_CODE
                + "\",\"tipoDte\":\"01\",\"ambiente\":\"00\",\"numeroControl\":\"DTE-01-M001P001-000000000000001\"},"
                + "\"emisor\":{\"nit\":\"0614-280390-112-1\"}}";
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        ReceptionRequest request = new ReceptionRequest("00", 1L, 1, "01", "e30." + encodedPayload + ".sig");
        when(schemaValidator.validate(any(JsonNode.class), eq("01"), isNull()))
                .thenReturn(java.util.List.of("$.resumen: required property not found"));

        DteResponse response = service.receive(request);

        ArgumentCaptor<DteEntity> savedDte = ArgumentCaptor.forClass(DteEntity.class);
        verify(repository).save(savedDte.capture());
        assertEquals("06142803901121", savedDte.getValue().getNitEmisor());
        assertEquals("01", savedDte.getValue().getTdte());
        assertEquals("RECHAZADO", savedDte.getValue().getEstado());
        assertNull(savedDte.getValue().getSelloRecibido());
        assertNull(savedDte.getValue().getClasificaMsg());
        assertEquals("ERROR_CODIGO", savedDte.getValue().getCodigoMsg());
        assertEquals("RECHAZADO POR REGLAS DE ESTRUCTURA Y SCHEMA", savedDte.getValue().getDescripcionMsg());
        assertEquals(java.util.List.of("$.resumen: required property not found"), response.observaciones());
    }

    @Test
    void receiveReturnsExistingDteWithoutSavingItAgain() throws Exception {
        String payload = "{\"identificacion\":{\"codigoGeneracion\":\"" + GENERATION_CODE + "\"}}";
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        ReceptionRequest request = new ReceptionRequest("00", 1L, 1, "01", "e30." + encodedPayload + ".sig");
        DteEntity existing = new DteEntity(GENERATION_CODE, "06142803901121", "01", "00", null,
                "EXISTING-SEAL", "PROCESADO", "10", "001", "RECIBIDO",
                "04/10/2026 20:16:36", payload, "[]");
        when(repository.findById(GENERATION_CODE)).thenReturn(Optional.of(existing));

        DteResponse response = service.receive(request);

        verify(repository, never()).save(any(DteEntity.class));
        assertEquals("EXISTING-SEAL", response.selloRecibido());
        assertEquals("04/10/2026 20:16:36", response.fhProcesamiento());
        assertEquals("PROCESADO", response.estado());
        assertEquals(java.util.List.of(), response.observaciones());
    }
}
