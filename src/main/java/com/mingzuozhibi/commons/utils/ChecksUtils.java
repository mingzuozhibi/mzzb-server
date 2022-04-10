package com.mingzuozhibi.commons.utils;

import com.mingzuozhibi.commons.gson.GsonFactory;
import com.mingzuozhibi.commons.result.BaseResult;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.Optional;

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
        return Optional.of(paramName + "必须输入");
    }

    public static Optional<String> checkSelected(Object value, String paramName) {
        if (!Objects.isNull(value)) {
            return Optional.empty();
        }
        return Optional.of(paramName + "必须选择");
    }

    public static Optional<String> checkIdentifier(String value, String paramName, int min, int max) {
        return checkMatches(value, "[A-Za-z0-9_]{" + min + "," + max + "}",
            paramName + "必须由英文、数字、下划线组成，且长度必须为" + min + "-" + max);
    }

    public static Optional<String> checkMd5Encode(String encode, String paramName, int length) {
        return checkMatches(encode, "[0-9a-f]{" + length + "}",
            paramName + "不是标准的" + length + "位Md5编码");
    }

    public static Optional<String> checkMatches(String text, String regex, String error) {
        if (text.matches(regex)) {
            return Optional.empty();
        }
        return Optional.of(error);
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

    private static String errorResult(String message) {
        return GsonFactory.GSON.toJson(new BaseResult(message));
    }

}
