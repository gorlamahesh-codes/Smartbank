package com.smartbank.model;

import java.math.BigDecimal;

public record AccountInfo(
        long accountId,
        String accountNumber,
        String accountType,
        BigDecimal balance,
        String status,
        long customerId
) {}
