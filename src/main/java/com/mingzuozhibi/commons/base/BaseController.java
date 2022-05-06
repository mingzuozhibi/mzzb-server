package com.mingzuozhibi.commons.base;

import com.mingzuozhibi.commons.domain.Result;
import com.mingzuozhibi.commons.domain.ResultPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;

import static org.springframework.web.context.request.RequestContextHolder.getRequestAttributes;

@Slf4j
public abstract class BaseController extends BaseSupport {

    protected static final String MEDIA_TYPE = MediaType.APPLICATION_JSON_VALUE;

    @ResponseBody
    @ExceptionHandler
    public String errorHandler(Exception e) throws Exception {
        if (e instanceof AccessDeniedException) {
            throw e;
        }
        if (e instanceof AuthenticationException) {
            throw e;
        }
        log.warn("errorHandler", e);
        return errorResult(e.toString());
    }

    private HttpServletRequest getRequest() {
        RequestAttributes attributes = getRequestAttributes();
        Objects.requireNonNull(attributes, "attributes is null");
        return ((ServletRequestAttributes) attributes).getRequest();
    }

    protected <T> String baseResult(Result<T> base) {
        return gson.toJson(base);
    }

    protected <T> String dataResult(T data) {
        return gson.toJson(Result.ofData(data));
    }

    protected <T> String pageResult(List<T> data, ResultPage page) {
        return gson.toJson(Result.ofPage(data, page));
    }

}
