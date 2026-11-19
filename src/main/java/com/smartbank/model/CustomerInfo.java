package com.smartbank.model;

public record CustomerInfo(
        long customerId,
        long userId,
        String fullName,
        String email,
        String phone,
        String address,
        String username,
        String role
) {}
