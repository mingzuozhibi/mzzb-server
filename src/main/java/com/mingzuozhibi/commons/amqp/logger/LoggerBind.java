package com.mingzuozhibi.commons.amqp.logger;

import com.mingzuozhibi.commons.amqp.AmqpEnums.Name;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface LoggerBind {
    Name value() default Name.DEFAULT;
}
