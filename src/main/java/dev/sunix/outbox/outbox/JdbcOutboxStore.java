package dev.sunix.outbox.outbox;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcOutboxStore implements OutboxStore {
    private final JdbcTemplate jdbc;

    public JdbcOutboxStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void append(UUID id, String aggregateType, UUID aggregateId, String eventType, String payload, Instant createdAt) {
        jdbc.update(
                """
                insert into outbox_event
                    (id, aggregate_type, aggregate_id, event_type, payload, created_at, available_at)
                values (?, ?, ?, ?, cast(? as jsonb), ?, ?)
                """,
                id, aggregateType, aggregateId, eventType, payload,
                Timestamp.from(createdAt), Timestamp.from(createdAt));
    }

    @Override
    public List<OutboxEvent> lockReady(int limit) {
        return jdbc.query(
                """
                select id, aggregate_id, event_type, payload::text, attempts, created_at
                  from outbox_event
                 where status = 'PENDING' and available_at <= clock_timestamp()
                 order by created_at, id
                 for update skip locked
                 limit ?
                """,
                JdbcOutboxStore::map,
                limit);
    }

    @Override
    public void markPublished(UUID id, Instant publishedAt) {
        jdbc.update("update outbox_event set status = 'PUBLISHED', published_at = ?, last_error = null where id = ? and status = 'PENDING'", Timestamp.from(publishedAt), id);
    }

    @Override
    public void scheduleRetry(UUID id, int attempts, Instant availableAt, String error) {
        jdbc.update("update outbox_event set attempts = ?, available_at = ?, last_error = ? where id = ? and status = 'PENDING'", attempts, Timestamp.from(availableAt), error, id);
    }

    @Override
    public void markDead(UUID id, int attempts, String error) {
        jdbc.update("update outbox_event set status = 'DEAD', attempts = ?, last_error = ? where id = ? and status = 'PENDING'", attempts, error, id);
    }

    private static OutboxEvent map(ResultSet rs, int row) throws SQLException {
        return new OutboxEvent(
                rs.getObject("id", UUID.class), rs.getObject("aggregate_id", UUID.class),
                rs.getString("event_type"), rs.getString("payload"), rs.getInt("attempts"),
                rs.getTimestamp("created_at").toInstant());
    }
}
