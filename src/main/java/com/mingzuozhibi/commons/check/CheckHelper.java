package com.mingzuozhibi.commons.check;

import org.apache.commons.lang3.StringUtils;

import java.util.function.Supplier;

public class CheckHelper {

    @SafeVarargs
    public static CheckResult runAllCheck(Supplier<CheckResult>... tasks) {
        for (Supplier<CheckResult> task : tasks) {
            CheckResult result = task.get();
            if (result.hasError()) {
                return result;
            }
        }
        return CheckResult.SUCCESS;
    }

    public static Supplier<CheckResult> checkNotEmpty(String value, String paramName) {
        return () -> {
            if (StringUtils.isNotEmpty(value)) {
                return CheckResult.SUCCESS;
            }
            return new CheckResult(paramName + "必须输入");
        };
    }

    public static Supplier<CheckResult> checkSelected(Object value, String paramName) {
        return () -> {
            if (value != null) {
                return CheckResult.SUCCESS;
            }
            return new CheckResult(paramName + "必须选择");
        };
    }

    public static Supplier<CheckResult> checkIdentifier(String value, String paramName, int min, int max) {
        return checkMatches(value, "[A-Za-z0-9_]{" + min + "," + max + "}",
            paramName + "必须由英文、数字、下划线组成，且长度必须为" + min + "-" + max);
    }

    public static Supplier<CheckResult> checkMd5Encode(String encode, String paramName, int length) {
        return checkMatchesOrEmpty(encode, "[0-9a-f]{" + length + "}",
            paramName + "不是标准的" + length + "位Md5编码");
    }

    public static Supplier<CheckResult> checkMatches(String text, String regex, String error) {
        return () -> {
            if (text.matches(regex)) {
                return CheckResult.SUCCESS;
            }
            return new CheckResult(error);
        };
    }

    public static Supplier<CheckResult> checkMatchesOrEmpty(String value, String regex, String error) {
        return () -> {
            if (value == null || value.matches(regex)) {
                return CheckResult.SUCCESS;
            }
            return new CheckResult(error);
        };
    }

}
