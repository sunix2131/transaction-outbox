package dev.sunix.outbox.outbox;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaEventSender implements EventSender {
    private final KafkaTemplate<String, String> kafka;
    private final String topic;
    private final Duration timeout;

    public KafkaEventSender(KafkaTemplate<String, String> kafka,
            @Value("${app.kafka.topic}") String topic,
            @Value("${app.outbox.send-timeout:5s}") Duration timeout) {
        this.kafka = kafka;
        this.topic = topic;
        this.timeout = timeout;
    }

    @Override
    public void send(OutboxEvent event) throws Exception {
        var record = new ProducerRecord<String, String>(topic, event.aggregateId().toString(), event.payload());
        record.headers().add("event_id", event.id().toString().getBytes(StandardCharsets.UTF_8));
        record.headers().add("event_type", event.eventType().getBytes(StandardCharsets.UTF_8));
        kafka.send(record).get(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }
}

