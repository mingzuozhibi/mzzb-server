package com.mingzuozhibi.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.Optional;

import static com.mingzuozhibi.commons.result.ResultSupport.errorResult;

public abstract class ChecksUtils {

    @SafeVarargs
    public static Optional<String> runChecks(Optional<String>... checks) {
        for (Optional<String> check : checks) {
            if (check.isPresent()) return check;
        }
        return Optional.empty();
    }

    public static Optional<String> checkNotEmpty(String value, String paramName) {
        if (StringUtils.isNotEmpty(value)) {
            return Optional.empty();
        }
        return Optional.of(paramName + "不能为空");
    }

    public static Optional<String> checkNotEmpty(Object value, String paramName) {
        if (!Objects.isNull(value)) {
            return Optional.empty();
        }
        return Optional.of(paramName + "不能为空");
    }

    public static Optional<String> checkStrMatch(String text, String paramName, String regex) {
        if (text == null || text.matches(regex)) {
            return Optional.empty();
        }
        return Optional.of(paramName + "必须符合格式" + regex);
    }

    public static String paramExists(String paramName) {
        return errorResult("指定的" + paramName + "已存在");
    }

    public static String paramNotExists(String paramName) {
        return errorResult("指定的" + paramName + "不存在");
    }

    public static String itemsExists(String itemName) {
        return errorResult("指定的" + itemName + "已存在于列表");
    }

    public static String itemsNotExists(String itemName) {
        return errorResult("指定的" + itemName + "不存在于列表");
    }

}
