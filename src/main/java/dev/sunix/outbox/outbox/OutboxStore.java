package dev.sunix.outbox.outbox;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxStore {
    void append(UUID id, String aggregateType, UUID aggregateId, String eventType, String payload, Instant createdAt);
    List<OutboxEvent> lockReady(int limit);
    void markPublished(UUID id, Instant publishedAt);
    void scheduleRetry(UUID id, int attempts, Instant availableAt, String error);
    void markDead(UUID id, int attempts, String error);
}

