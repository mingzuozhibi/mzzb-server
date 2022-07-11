package com.mingzuozhibi.support;

import java.io.*;
import java.nio.charset.StandardCharsets;

public abstract class FileIoUtils {

    public static void writeLine(String fileName, String text) {
        try (
            var fos = new FileOutputStream(fileName, true);
            var osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
            var pw = new PrintWriter(new BufferedWriter(osw), true)
        ) {
            pw.println(text);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
