package dev.sunix.outbox.payment;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentServiceTest {
    @Test
    void requestHashUsesCanonicalMoneyRepresentation() {
        UUID accountId = UUID.fromString("3b239b56-c8ef-4ca1-af51-83ac019be6dd");

        String first = PaymentService.requestHash(accountId, new BigDecimal("10.00"), "RUB");
        String second = PaymentService.requestHash(accountId, new BigDecimal("10.00"), "RUB");

        assertThat(first).isEqualTo(second).hasSize(64);
        assertThat(first).isNotEqualTo(PaymentService.requestHash(accountId, new BigDecimal("10.01"), "RUB"));
    }
}

