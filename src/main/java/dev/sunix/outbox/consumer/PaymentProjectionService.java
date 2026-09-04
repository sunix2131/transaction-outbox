package dev.sunix.outbox.consumer;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Service
public class PaymentProjectionService {
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public PaymentProjectionService(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Transactional
    public boolean apply(JsonNode event) {
        UUID eventId = UUID.fromString(event.required("eventId").stringValue());
        UUID paymentId = UUID.fromString(event.required("paymentId").stringValue());
        int inserted = jdbc.update(
                "insert into processed_event (event_id, processed_at) values (?, ?) on conflict do nothing",
                eventId, Timestamp.from(clock.instant()));
        if (inserted == 0) {
            return false;
        }
        jdbc.update(
                """
                insert into payment_projection
                    (payment_id, account_id, amount, currency, accepted_at, applied_count)
                values (?, ?, ?, ?, ?, 1)
                on conflict (payment_id) do update
                    set applied_count = payment_projection.applied_count + 1
                """,
                paymentId,
                UUID.fromString(event.required("accountId").stringValue()),
                new BigDecimal(event.required("amount").stringValue()),
                event.required("currency").stringValue(),
                Timestamp.from(Instant.parse(event.required("occurredAt").stringValue())));
        return true;
    }
}
