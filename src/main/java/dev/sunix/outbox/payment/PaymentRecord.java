package dev.sunix.outbox.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentRecord(
        UUID id,
        String requestHash,
        UUID accountId,
        BigDecimal amount,
        String currency,
        String status,
        Instant createdAt) {}

