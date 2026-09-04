package dev.sunix.outbox.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class PaymentProjectionConsumer {
    private final ObjectMapper objectMapper;
    private final PaymentProjectionService projection;

    public PaymentProjectionConsumer(ObjectMapper objectMapper, PaymentProjectionService projection) {
        this.objectMapper = objectMapper;
        this.projection = projection;
    }

    @KafkaListener(topics = "${app.kafka.topic}")
    public void receive(String payload) throws Exception {
        JsonNode event = objectMapper.readTree(payload);
        if (!"payment.accepted.v1".equals(event.required("eventType").stringValue())) {
            throw new IllegalArgumentException("unsupported event type");
        }
        projection.apply(event);
    }
}
