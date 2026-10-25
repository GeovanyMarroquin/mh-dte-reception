package com.digital.reception.dto;

public record AnulacionRequest(String ambiente, Long idEnvio, Integer version, String documento) {
}
