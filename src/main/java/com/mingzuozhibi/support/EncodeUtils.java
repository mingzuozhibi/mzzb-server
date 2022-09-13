package com.mingzuozhibi.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;

public abstract class EncodeUtils {

    public static String encodePassword(String username, String password) {
        Objects.requireNonNull(username);
        Objects.requireNonNull(password);
        return md5(username + md5(password));
    }

    private static String md5(String text) {
        try {
            var md = MessageDigest.getInstance("MD5");
            var bytes = md.digest(text.getBytes(StandardCharsets.UTF_8));
            return toHex(bytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String toHex(byte[] bytes) {
        final var HEX_DIGITS = "0123456789abcdef".toCharArray();
        var ret = new StringBuilder(bytes.length * 2);
        for (var b : bytes) {
            ret.append(HEX_DIGITS[(b >> 4) & 0x0f]);
            ret.append(HEX_DIGITS[b & 0x0f]);
        }
        return ret.toString();
    }

}
