package com.digital.reception.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class DteSchemaValidatorService {
    private static final Map<String, String> SCHEMA_BY_TYPE = Map.ofEntries(
            Map.entry("01", "fe-f-v2.json"),
            Map.entry("03", "fe-ccf-v4.json"),
            Map.entry("04", "fe-nr-v4.json"),
            Map.entry("05", "fe-nc-v4.json"),
            Map.entry("06", "fe-nd-v4.json"),
            Map.entry("07", "fe-cr-v2.json"),
            Map.entry("08", "fe-dcl-v2.json"),
            Map.entry("09", "fe-cl-v2.json"),
            Map.entry("11", "fe-fex-v3.json"),
            Map.entry("14", "fe-fse-v2.json"),
            Map.entry("15", "fe-cd-v2.json")
    );
    private static final Map<String, String> EVENT_SCHEMAS = Map.of(
            "operaciones", "fe-eop-v1.json", "retorno", "fe-eret-v1.json",
            "invalidacion", "invalidacion-schema-v3.json", "contingencia", "contingencia-schema-v4.json");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, JsonSchema> schemas = new HashMap<>();

    @PostConstruct
    void loadSchemas() throws IOException {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath*:schemas/**/*.json");
        for (Resource resource : resources) {
            String fileName = resource.getFilename();
            try (var input = resource.getInputStream()) {
                schemas.put(fileName, factory.getSchema(objectMapper.readTree(input)));
            }
        }
        if (schemas.size() != 15) {
            throw new IllegalStateException("Se esperaban 15 JSON schemas y se cargaron " + schemas.size());
        }
    }

    public List<String> validate(JsonNode document, String type, String eventContext) {
        String fileName = resolveSchema(type, eventContext);
        JsonSchema schema = schemas.get(fileName);
        if (schema == null) {
            return List.of("No existe schema configurado para el tipo/contexto: " + type);
        }
        Set<ValidationMessage> errors = schema.validate(document);
        List<String> observations = new ArrayList<>();
        errors.stream().map(ValidationMessage::getMessage).sorted(Comparator.naturalOrder()).forEach(observations::add);
        return observations;
    }

    private String resolveSchema(String type, String eventContext) {
        if (eventContext != null && !eventContext.isBlank()) {
            String normalized = eventContext.toLowerCase().replaceAll("[^a-z]", "");
            for (Map.Entry<String, String> entry : EVENT_SCHEMAS.entrySet()) {
                if (normalized.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }
        }
        return type == null ? null : SCHEMA_BY_TYPE.get(type);
    }
}
