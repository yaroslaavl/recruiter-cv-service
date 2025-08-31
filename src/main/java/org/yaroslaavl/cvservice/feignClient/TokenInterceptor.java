package org.yaroslaavl.cvservice.feignClient;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenInterceptor implements RequestInterceptor {

    private final TokenManager tokenManager;
    private static final String TOKEN = "Bearer {0}";

    @Override
    public void apply(RequestTemplate requestTemplate) {
        String formattedToken = MessageFormat.format(TOKEN, tokenManager.getServiceToken());
        requestTemplate.header("Authorization", formattedToken);
    }
}
