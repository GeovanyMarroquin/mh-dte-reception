package com.digital.reception.service;

import com.digital.reception.dto.AnulacionRequest;
import com.digital.reception.dto.AnulacionResponse;
import com.digital.reception.dto.ConsultaDteRequest;
import com.digital.reception.dto.ConsultDteResult;
import com.digital.reception.dto.DteResponse;
import com.digital.reception.dto.ReceptionRequest;
import com.digital.reception.entity.DteEntity;
import com.digital.reception.repository.DteRepository;
import com.digital.reception.util.JwsDecoderUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DteService {
    private static final DateTimeFormatter PROCESSING_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DteSchemaValidatorService schemaValidator;
    private final DteRepository repository;
    private final Clock clock = Clock.systemDefaultZone();
    private static final String PROCESADO = "PROCESADO";
    private static final String RECHAZADO = "RECHAZADO";

    @Transactional
    public DteResponse receive(ReceptionRequest request) throws Exception {
        String documentJson = JwsDecoderUtil.decodePayload(request.documento());
        JsonNode dte = objectMapper.readTree(documentJson);
        JsonNode identification = dte.path("identificacion");
        String tipoDte = textOrNull(identification.path("tipoDte"));
        if (tipoDte == null) tipoDte = request.tipoDte();
        String codigoGeneracion = textOrNull(identification.path("codigoGeneracion"));
        if (codigoGeneracion == null) codigoGeneracion = UUID.randomUUID().toString().toUpperCase();

        DteEntity existingReception = repository.findById(codigoGeneracion).orElse(null);
        if (existingReception != null) {
            return toReceiveResponse(existingReception);
        }

        String eventContext = textOrNull(dte.path("tipoEvento"));
        if (eventContext == null) eventContext = textOrNull(dte.path("evento"));

        List<String> observations = schemaValidator.validate(dte, tipoDte, eventContext);
        boolean valid = observations.isEmpty();
        String nitEmisor = textOrNull(dte.path("emisor").path("nit"));
        if (nitEmisor != null) nitEmisor = nitEmisor.replace("-", "");
        String ambiente = firstNonBlank(textOrNull(identification.path("ambiente")), request.ambiente(), "00");
        String numeroControl = textOrNull(identification.path("numeroControl"));
        String selloRecibido = valid ? newReceiptSeal() : null;
        String processingTime = LocalDateTime.now(clock).format(PROCESSING_TIME);
        String estado = valid ? PROCESADO : RECHAZADO;
        String clasificaMsg = valid ? "10" : null;
        String codigoMsg = valid ? "001" : "ERROR_CODIGO";
        String descripcionMsg = valid ? "RECIBIDO" : "RECHAZADO POR REGLAS DE ESTRUCTURA Y SCHEMA";

        DteEntity reception = new DteEntity(codigoGeneracion, nitEmisor, tipoDte, ambiente, numeroControl,
                selloRecibido, estado, clasificaMsg, codigoMsg, descripcionMsg, processingTime,
                objectMapper.writeValueAsString(dte), objectMapper.writeValueAsString(observations));
        repository.save(reception);

        return new DteResponse(2, ambiente, 2, reception.getEstado(), codigoGeneracion,
                selloRecibido, processingTime, clasificaMsg, codigoMsg, descripcionMsg, observations);
    }

    private DteResponse toReceiveResponse(DteEntity reception) throws Exception {
        List<String> observations = objectMapper.readValue(
                reception.getObservaciones(), new TypeReference<>() {
                });
        return getResponse(reception, observations);
    }

    public AnulacionResponse annul(AnulacionRequest request) {
        String envelopeAmbiente = request == null ? null : request.ambiente();
        try {
            if (request == null || request.documento() == null || request.documento().isBlank()) {
                throw new IllegalArgumentException("El documento JWS es obligatorio");
            }
            String eventJson = JwsDecoderUtil.decodePayload(request.documento());
            JsonNode evento = objectMapper.readTree(eventJson);
            JsonNode identificacion = evento.path("identificacion");
            String codigoGeneracion = textOrNull(identificacion.path("codigoGeneracion"));
            String ambiente = firstNonBlank(textOrNull(identificacion.path("ambiente")), envelopeAmbiente, "00");

            List<String> observations = schemaValidator.validate(evento, null, "invalidacion");
            if (observations.isEmpty()) {
                String processingTime = LocalDateTime.now(clock).format(PROCESSING_TIME);
                return new AnulacionResponse(2, ambiente, 1, PROCESADO, codigoGeneracion,
                        newReceiptSeal(), processingTime, "001", "Invalidación Recibida y Procesada", null);
            }
            return rejectedAnnulment(codigoGeneracion, ambiente, observations);
        } catch (Exception exception) {
            String message = exception.getMessage() == null ? exception.toString() : exception.getMessage();
            return rejectedAnnulment(null, firstNonBlank(envelopeAmbiente, "00"), List.of(message));
        }
    }

    private static AnulacionResponse rejectedAnnulment(String codigoGeneracion, String ambiente,
                                                      List<String> observations) {
        return new AnulacionResponse(2, ambiente, 1, RECHAZADO, codigoGeneracion, null, null,
                "ERROR_CODIGO", "RECHAZADO POR REGLAS DE ESTRUCTURA Y SCHEMA", observations);
    }

    @Transactional
    public ConsultDteResult consult(ConsultaDteRequest request) {
        String codigoGeneracion = request.codigoGeneracion();
        if (codigoGeneracion == null || codigoGeneracion.isBlank()
                || request.nitEmisor() == null || request.nitEmisor().isBlank()
                || request.tdte() == null || request.tdte().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "nitEmisor, tdte y codigoGeneracion son obligatorios"
            );
        }

        String nitEmisor = request.nitEmisor().replace("-", "");
        return repository.findByCodigoGeneracionAndNitEmisorAndTdte(codigoGeneracion, nitEmisor, request.tdte())
                .map(this::toConsultResult)
                .orElseGet(() -> notFoundResult(codigoGeneracion));
    }

    private ConsultDteResult toConsultResult(DteEntity reception) {
        try {
            List<String> observations = objectMapper.readValue(reception.getObservaciones(), new TypeReference<>() {
            });
            boolean processed = PROCESADO.equals(reception.getEstado());
            DteResponse response = getResponse(reception, processed ? observations : null);
            return new ConsultDteResult(processed ? 200 : 400, response);
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudieron leer las observaciones persistidas", exception);
        }
    }

    private static @NonNull DteResponse getResponse(DteEntity reception, List<String> observations) {
        return new DteResponse(
                2,
                reception.getAmbiente(),
                2,
                reception.getEstado(),
                reception.getCodigoGeneracion(),
                reception.getSelloRecibido(),
                reception.getFhProcesamiento(),
                reception.getClasificaMsg(),
                reception.getCodigoMsg(),
                reception.getDescripcionMsg(),
                observations
        );
    }

    private ConsultDteResult notFoundResult(String codigoGeneracion) {
        DteResponse response = new DteResponse(2,
                "00", 2, RECHAZADO, codigoGeneracion, null,
                LocalDateTime.now(clock).format(PROCESSING_TIME), null, "301", "DOCUMENTO NO EXISTE",
                null);
        return new ConsultDteResult(400, response);
    }

    private String newReceiptSeal() {
        String first = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        String second = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return first + second.substring(0, 8);
    }

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) return candidate;
        }
        return "00";
    }

    private String textOrNull(JsonNode node) {
        return node != null && node.isTextual() && !node.asText().isBlank() ? node.asText() : null;
    }
}
