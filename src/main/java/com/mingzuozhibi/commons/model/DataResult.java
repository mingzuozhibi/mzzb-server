package com.mingzuozhibi.commons.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DataResult<T> extends ErrorResult {

    private T data;

    public DataResult(T data) {
        setSuccess(true);
        this.data = data;
    }

}
