package mingzuozhibi.security;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

public class CustomAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        UsernamePasswordAuthenticationToken authRequest;
        try (InputStream is = request.getInputStream()) {
            DocumentContext context = JsonPath.parse(is);
            String username = context.read("$.username", String.class);
            String password = context.read("$.password", String.class);
            authRequest = new UsernamePasswordAuthenticationToken(username, password);
            LoggerFactory.getLogger(CustomAuthenticationFilter.class)
                    .info("CustomAuthenticationFilter Running");
        } catch (IOException e) {
            e.printStackTrace();
            authRequest = new UsernamePasswordAuthenticationToken("", "");
        }
        setDetails(request, authRequest);
        return this.getAuthenticationManager().authenticate(authRequest);
    }

}
