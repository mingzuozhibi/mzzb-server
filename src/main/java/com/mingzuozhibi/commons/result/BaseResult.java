package com.mingzuozhibi.commons.result;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BaseResult {

    private boolean success;
    private String message;

    public BaseResult(String message) {
        this.success = false;
        this.message = message;
    }

}
