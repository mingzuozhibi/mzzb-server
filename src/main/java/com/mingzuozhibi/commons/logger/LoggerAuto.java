package com.mingzuozhibi.commons.logger;

import com.mingzuozhibi.commons.amqp.AmqpSender;
import com.mingzuozhibi.commons.base.BaseKeys.Name;
import com.mingzuozhibi.commons.base.BaseSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoggerAuto implements BeanPostProcessor {

    @Autowired
    private AmqpSender amqpSender;

    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof BaseSupport) {
            try {
                var beanClass = bean.getClass();
                var loggerBind = beanClass.getAnnotation(LoggerBind.class);
                var name = loggerBind != null ? loggerBind.value() : Name.DEFAULT;
                var setBind = beanClass.getMethod("setBind", Logger.class);
                setBind.invoke(bean, amqpSender.bind(name));
                log.debug("LoggerBind: bean=%s, name=%s".formatted(beanName, name.name()));
            } catch (Exception ignored) {
            }
        }
        return bean;
    }

}
