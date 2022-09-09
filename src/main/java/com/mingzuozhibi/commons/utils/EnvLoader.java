package com.mingzuozhibi.commons.utils;

import java.util.Objects;

public class EnvLoader {

    private static final boolean DEV_MODE = Objects.equals(System.getenv("DEV_MODE"), "TRUE");

    public static boolean isDevMode() {
        return DEV_MODE;
    }

}
