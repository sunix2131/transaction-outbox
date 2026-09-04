package dev.sunix.outbox.outbox;

import java.time.Instant;
import java.util.UUID;

public record OutboxEvent(
        UUID id, UUID aggregateId, String eventType, String payload, int attempts, Instant createdAt) {}

