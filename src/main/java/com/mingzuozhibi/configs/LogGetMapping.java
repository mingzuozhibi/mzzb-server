package com.mingzuozhibi.configs;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class LogGetMapping {

    @Pointcut(value = "@annotation(org.springframework.web.bind.annotation.GetMapping)")
    public void pointCut() {
    }

    @Around(value = "pointCut()")
    public Object aroundPringLog(ProceedingJoinPoint joinPoint) throws Throwable {
        var sw = new StopWatch();
        sw.start();
        var proceed = joinPoint.proceed(joinPoint.getArgs());
        sw.stop();
        log.debug("Log @GetMapping: %s %dms".formatted(joinPoint.getSignature(), sw.getTime()));
        return proceed;
    }

}
