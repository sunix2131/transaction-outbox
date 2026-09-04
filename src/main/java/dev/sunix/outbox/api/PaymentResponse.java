package dev.sunix.outbox.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID accountId,
        BigDecimal amount,
        String currency,
        String status,
        Instant createdAt,
        boolean replayed) {}

