package com.example.demo.util;

import java.util.Locale;

public final class UniqueTextNormalizer {
    private UniqueTextNormalizer() {
    }

    public static String normalizeForUnique(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }
}
