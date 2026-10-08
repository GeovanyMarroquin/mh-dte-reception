package com.digital.reception.dto;

public record ReceptionRequest(String ambiente, Long idEnvio, Integer version, String tipoDte, String documento) {
}
