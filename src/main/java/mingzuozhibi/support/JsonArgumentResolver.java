package mingzuozhibi.support;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.core.MethodParameter;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class JsonArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String JSON_BODY_ATTRIBUTE = "JSON_REQUEST_BODY";

    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(JsonArg.class);
    }

    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        String body = getRequestBody(webRequest);
        String arg = getParameterName(parameter);
        String defaults = getDefaults(parameter);
        return readObject(body, arg, defaults, parameter.getParameterType());
    }

    private <T> T readObject(String body, String name, String defaults, Class<T> requiredType) {
        SimpleTypeConverter converter = new SimpleTypeConverter();
        try {
            Class<?> readType = requiredType.isEnum() ? String.class : requiredType;
            Object read = JsonPath.parse(body).read(name, readType);
            return converter.convertIfNecessary(read, requiredType);
        } catch (PathNotFoundException e) {
            return converter.convertIfNecessary(defaults, requiredType);
        }
    }

    private String getParameterName(MethodParameter parameter) {
        String name = parameter.getParameterAnnotation(JsonArg.class).value();
        if (StringUtils.isEmpty(name)) {
            name = parameter.getParameterName();
        }
        return name;
    }

    private String getDefaults(MethodParameter parameter) {
        return parameter.getParameterAnnotation(JsonArg.class).defaults();
    }

    private String getRequestBody(NativeWebRequest webRequest) {
        HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);

        String jsonBody = (String) webRequest.getAttribute(JSON_BODY_ATTRIBUTE, NativeWebRequest.SCOPE_REQUEST);
        if (jsonBody == null) {
            try {
                jsonBody = IOUtils.toString(servletRequest.getInputStream(), "UTF-8");
                webRequest.setAttribute(JSON_BODY_ATTRIBUTE, jsonBody, NativeWebRequest.SCOPE_REQUEST);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return jsonBody;
    }

}
