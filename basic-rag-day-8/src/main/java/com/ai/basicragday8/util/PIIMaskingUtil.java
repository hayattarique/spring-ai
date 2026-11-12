package com.ai.basicragday8.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PIIMaskingUtil {

    private PIIMaskingUtil() {
        // Prevent object creation
    }

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile(
                    "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b"
            );

    private static final Pattern PHONE_PATTERN =
            Pattern.compile(
                    "(?<!\\d)(?:\\+91[- ]?)?[6-9]\\d{9}(?!\\d)"
            );

    private static final Pattern AADHAAR_PATTERN =
            Pattern.compile("(?<!\\d)\\d{12}(?!\\d)");

    private static final Pattern PAN_PATTERN =
            Pattern.compile(
                    "\\b[A-Z]{5}\\d{4}[A-Z]\\b",
                    Pattern.CASE_INSENSITIVE
            );

    public static String maskSensitiveData(String text) {

        if (text == null || text.isBlank()) {
            return text;
        }

        text = replace(text, EMAIL_PATTERN,
                PIIMaskingUtil::maskEmail);

        text = replace(text, PHONE_PATTERN,
                value -> maskExceptLast(value, 4));


        return text;
    }

    private static String maskEmail(String email) {

        int atIndex = email.indexOf('@');

        String username = email.substring(0, atIndex);

        return username.charAt(0)
                + "*".repeat(username.length() - 1)
                + email.substring(atIndex);
    }

    private static String maskExceptLast(
            String value, int visibleCharacters) {

        if (value.length() <= visibleCharacters) {
            return "X".repeat(value.length());
        }

        return "X".repeat(value.length() - visibleCharacters)
                + value.substring(value.length() - visibleCharacters);
    }

    private static String replace(
            String text,
            Pattern pattern,
            java.util.function.Function<String, String> masker) {

        Matcher matcher = pattern.matcher(text);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            matcher.appendReplacement(
                    result,
                    Matcher.quoteReplacement(masker.apply(matcher.group()))
            );
        }

        matcher.appendTail(result);

        return result.toString();
    }
}
