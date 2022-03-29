package com.mingzuozhibi.commons.check;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CheckResult {

    public static final CheckResult SUCCESS = new CheckResult();

    private String error;

    public boolean hasError() {
        return error != null;
    }

}
