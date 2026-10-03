package com.example.positionbook.api.response;

/** OpenAPI schema-only representation details response. */
public record ApiProblemResponse(String type, String title, int status, String detail, String instance, String code) {
}
