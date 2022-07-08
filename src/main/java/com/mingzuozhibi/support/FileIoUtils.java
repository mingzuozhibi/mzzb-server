package com.mingzuozhibi.support;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

public abstract class FileIoUtils {

    public static void writeLine(String fileName, String text) {
        try (PrintWriter pw = new PrintWriter(fileName, StandardCharsets.UTF_8)) {
            pw.println(text);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
