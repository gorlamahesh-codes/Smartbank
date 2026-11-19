package com.smartbank.security;

public class PasswordGenerator {

    public static void main(String[] args) {

        String password = "root1234";

        String hashedPassword = PasswordUtil.hash(password);

        System.out.println("Username: root");
        System.out.println("Password: " + password);
        System.out.println("Password Hash: " + hashedPassword);
    }
}