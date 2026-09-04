package dev.sunix.outbox.outbox;

public interface EventSender {
    void send(OutboxEvent event) throws Exception;
}

