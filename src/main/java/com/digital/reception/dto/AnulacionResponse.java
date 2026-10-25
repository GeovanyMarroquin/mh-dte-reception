package com.digital.reception.dto;

import java.util.List;

public record AnulacionResponse(
        int version,
        String ambiente,
        int versionApp,
        String estado,
        String codigoGeneracion,
        String selloRecibido,
        String fhProcesamiento,
        String codigoMsg,
        String descripcionMsg,
        List<String> observaciones
) {
}
