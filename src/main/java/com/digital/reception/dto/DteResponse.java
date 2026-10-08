package com.digital.reception.dto;

import java.util.List;

public record DteResponse(
        int version,
        String ambiente,
        int versionApp,
        String estado,
        String codigoGeneracion,
        String selloRecibido,
        String fhProcesamiento,
        String clasificaMsg,
        String codigoMsg,
        String descripcionMsg,
        List<String> observaciones
) {
}
