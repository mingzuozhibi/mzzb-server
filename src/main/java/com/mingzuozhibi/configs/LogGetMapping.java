package com.mingzuozhibi.configs;

import com.mingzuozhibi.commons.base.BaseSupport;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.StringJoiner;

@Slf4j
@Aspect
@Component
public class LogGetMapping extends BaseSupport {

    @Pointcut(value = "@annotation(org.springframework.web.bind.annotation.GetMapping)")
    public void pointCut() {
    }

    @Around(value = "pointCut()")
    public Object aroundPringLog(ProceedingJoinPoint joinPoint) throws Throwable {
        var stopWatch = new StopWatch();
        stopWatch.start();
        var proceed = joinPoint.proceed(joinPoint.getArgs());
        stopWatch.stop();
        log.debug("Log @GetMapping: %s %s %dms".formatted(
            joinPoint.getSignature(),
            formatArgs(joinPoint),
            stopWatch.getTime()
        ));
        return proceed;
    }

    private String formatArgs(ProceedingJoinPoint joinPoint) {
        var args = joinPoint.getArgs();
        if (args.length == 0) return "[<noargs>]";
        try {
            var names = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
            var joiner = new StringJoiner(",", "[", "]");
            for (var i = 0; i < args.length; i++) {
                joiner.add("%s=%s".formatted(names[i], gson.toJson(args[i])));
            }
            return joiner.toString();
        } catch (Exception e) {
            return "[<error>]";
        }
    }

}
