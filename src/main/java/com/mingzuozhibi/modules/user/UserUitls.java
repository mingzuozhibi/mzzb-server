package com.mingzuozhibi.modules.user;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;

public abstract class UserUitls {

    public static String encode(String username, String password) {
        Objects.requireNonNull(username);
        Objects.requireNonNull(password);
        return md5(username + md5(password));
    }

    private static String md5(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(text.getBytes(StandardCharsets.UTF_8));
            return toHex(bytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String toHex(byte[] bytes) {
        final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();
        StringBuilder ret = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            ret.append(HEX_DIGITS[(b >> 4) & 0x0f]);
            ret.append(HEX_DIGITS[b & 0x0f]);
        }
        return ret.toString();
    }

}
