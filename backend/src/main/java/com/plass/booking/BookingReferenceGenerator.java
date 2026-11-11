package com.plass.booking;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

/** Short, readable ticket codes such as "PLS-7KQ2XM" (no 0/O or 1/I to avoid confusion). */
@Component
public class BookingReferenceGenerator {

    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int LENGTH = 6;

    private final SecureRandom random = new SecureRandom();

    public String next() {
        StringBuilder reference = new StringBuilder("PLS-");
        for (int i = 0; i < LENGTH; i++) {
            reference.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        }
        return reference.toString();
    }
}
