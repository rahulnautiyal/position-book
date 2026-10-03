package com.example.positionbook.domain;

public record PositionKey(String tradingAccount, String securityId) {
    public PositionKey {
        if (tradingAccount == null || tradingAccount.isBlank()) throw new IllegalArgumentException("tradingAccount is required");
        if (securityId == null || securityId.isBlank()) throw new IllegalArgumentException("securityId is required");
    }
}
