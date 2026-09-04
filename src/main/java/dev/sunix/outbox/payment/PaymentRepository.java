package dev.sunix.outbox.payment;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentRepository {
    private final JdbcTemplate jdbc;

    public PaymentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void lockIdempotencyKey(String key) {
        jdbc.queryForObject(
                "select pg_advisory_xact_lock(hashtextextended(?, 0))",
                (resultSet, rowNumber) -> Boolean.TRUE,
                key);
    }

    public Optional<PaymentRecord> findByIdempotencyKey(String key) {
        return jdbc.query(
                        """
                        select id, request_hash, account_id, amount, currency, status, created_at
                          from payment where idempotency_key = ?
                        """,
                        PaymentRepository::map,
                        key)
                .stream()
                .findFirst();
    }

    public void insert(
            UUID id,
            String idempotencyKey,
            String requestHash,
            UUID accountId,
            BigDecimal amount,
            String currency,
            Instant createdAt) {
        jdbc.update(
                """
                insert into payment
                    (id, idempotency_key, request_hash, account_id, amount, currency, status, created_at)
                values (?, ?, ?, ?, ?, ?, 'ACCEPTED', ?)
                """,
                id, idempotencyKey, requestHash, accountId, amount, currency, Timestamp.from(createdAt));
    }

    private static PaymentRecord map(ResultSet rs, int row) throws SQLException {
        return new PaymentRecord(
                rs.getObject("id", UUID.class), rs.getString("request_hash"),
                rs.getObject("account_id", UUID.class), rs.getBigDecimal("amount"),
                rs.getString("currency"), rs.getString("status"),
                rs.getTimestamp("created_at").toInstant());
    }
}
