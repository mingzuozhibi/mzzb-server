package com.mingzuozhibi.commons.logger;

import com.mingzuozhibi.commons.base.BaseKeys.Name;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface LoggerBind {
    Name value() default Name.DEFAULT;
}
