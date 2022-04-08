package com.mingzuozhibi.commons.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ErrorResult {

    private boolean success;
    private String message;

    public ErrorResult(String message) {
        this.success = false;
        this.message = message;
    }

}
