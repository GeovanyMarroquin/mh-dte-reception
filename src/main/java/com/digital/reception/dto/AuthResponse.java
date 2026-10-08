package com.digital.reception.dto;

import java.util.List;

public record AuthResponse(String status, Body body) {
    public record Body(String token, List<String> roles, String tokenType) {
    }
}
