package dev.sunix.outbox.payment;

import dev.sunix.outbox.api.CreatePaymentRequest;
import dev.sunix.outbox.api.PaymentResponse;
import dev.sunix.outbox.outbox.OutboxStore;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class PaymentService {
    private final PaymentRepository payments;
    private final OutboxStore outbox;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public PaymentService(PaymentRepository payments, OutboxStore outbox, ObjectMapper objectMapper, Clock clock) {
        this.payments = payments;
        this.outbox = outbox;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public PaymentResponse create(String idempotencyKey, CreatePaymentRequest request) {
        String normalizedKey = idempotencyKey.strip();
        BigDecimal amount;
        try {
            amount = request.amount().setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("amount must have at most two fractional digits");
        }
        String currency = request.currency().toUpperCase(Locale.ROOT);
        String requestHash = requestHash(request.accountId(), amount, currency);

        // This transaction-scoped lock closes the race between lookup and insert.
        // Different idempotency keys do not block each other.
        payments.lockIdempotencyKey(normalizedKey);
        var existing = payments.findByIdempotencyKey(normalizedKey);
        if (existing.isPresent()) {
            PaymentRecord payment = existing.get();
            if (!payment.requestHash().equals(requestHash)) {
                throw new IdempotencyConflictException();
            }
            return response(payment, true);
        }

        UUID paymentId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        Instant now = clock.instant();
        payments.insert(paymentId, normalizedKey, requestHash, request.accountId(), amount, currency, now);
        outbox.append(eventId, "payment", paymentId, "payment.accepted.v1",
                eventPayload(eventId, paymentId, request.accountId(), amount, currency, now), now);
        return new PaymentResponse(paymentId, request.accountId(), amount, currency, "ACCEPTED", now, false);
    }

    private PaymentResponse response(PaymentRecord payment, boolean replayed) {
        return new PaymentResponse(payment.id(), payment.accountId(), payment.amount(), payment.currency(),
                payment.status(), payment.createdAt(), replayed);
    }

    private String eventPayload(UUID eventId, UUID paymentId, UUID accountId, BigDecimal amount,
            String currency, Instant occurredAt) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("eventId", eventId);
        event.put("eventType", "payment.accepted.v1");
        event.put("paymentId", paymentId);
        event.put("accountId", accountId);
        event.put("amount", amount.toPlainString());
        event.put("currency", currency);
        event.put("occurredAt", occurredAt);
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JacksonException exception) {
            throw new IllegalStateException("could not serialize outbox event", exception);
        }
    }

    static String requestHash(UUID accountId, BigDecimal amount, String currency) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] canonical = (accountId + "\n" + amount.toPlainString() + "\n" + currency)
                    .getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(digest.digest(canonical));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
