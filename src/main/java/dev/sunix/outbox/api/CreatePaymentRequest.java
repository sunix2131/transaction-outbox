package dev.sunix.outbox.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentRequest(
        @NotNull UUID accountId,
        @NotNull @Positive BigDecimal amount,
        @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency) {}

