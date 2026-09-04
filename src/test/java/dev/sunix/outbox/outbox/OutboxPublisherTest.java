package dev.sunix.outbox.outbox;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OutboxPublisherTest {
    private static final Instant NOW = Instant.parse("2026-01-15T12:00:00Z");
    private final OutboxStore store = mock(OutboxStore.class);
    private final EventSender sender = mock(EventSender.class);
    private final OutboxPublisher publisher =
            new OutboxPublisher(store, sender, Clock.fixed(NOW, ZoneOffset.UTC), 10, 3);

    @Test
    void marksAcknowledgedEventAsPublished() throws Exception {
        OutboxEvent event = event(0);
        when(store.lockReady(10)).thenReturn(List.of(event));

        publisher.publishBatch();

        verify(sender).send(event);
        verify(store).markPublished(event.id(), NOW);
    }

    @Test
    void schedulesBoundedBackoffAfterTransientFailure() throws Exception {
        OutboxEvent event = event(0);
        when(store.lockReady(10)).thenReturn(List.of(event));
        org.mockito.Mockito.doThrow(new IllegalStateException("broker unavailable"))
                .when(sender)
                .send(event);

        publisher.publishBatch();

        verify(store).scheduleRetry(event.id(), 1, NOW.plusSeconds(1), "IllegalStateException: broker unavailable");
    }

    @Test
    void movesEventToDeadStateAfterLastAttempt() throws Exception {
        OutboxEvent event = event(2);
        when(store.lockReady(10)).thenReturn(List.of(event));
        org.mockito.Mockito.doThrow(new IllegalStateException("still unavailable"))
                .when(sender)
                .send(event);

        publisher.publishBatch();

        verify(store).markDead(event.id(), 3, "IllegalStateException: still unavailable");
    }

    private static OutboxEvent event(int attempts) {
        return new OutboxEvent(UUID.randomUUID(), UUID.randomUUID(), "payment.accepted.v1", "{}", attempts, NOW);
    }
}

