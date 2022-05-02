package com.mingzuozhibi.configs;

import com.mingzuozhibi.commons.base.BaseSupport;
import com.mingzuozhibi.commons.mylog.*;
import com.mingzuozhibi.commons.mylog.JmsEnums.Name;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Component
public class AutoBindLogger implements BeanPostProcessor {

    @Autowired
    private JmsSender jmsSender;

    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof BaseSupport) {
            try {
                Class<?> beanClass = bean.getClass();
                Name name = beanClass.getAnnotation(JmsBind.class).value();
                Method setBind = beanClass.getMethod("setBind", JmsLogger.class);
                setBind.invoke(bean, jmsSender.bind(name));
                log.debug("JmsBind: bean={}, name={}", beanName, name.name());
            } catch (Exception ignored) {
            }
        }
        return bean;
    }

}
