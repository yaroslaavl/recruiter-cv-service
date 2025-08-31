package org.yaroslaavl.cvservice.feignClient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.yaroslaavl.cvservice.exception.ServiceTokenException;

import java.util.Map;

@Slf4j
@Component
public class TokenManager {

    @Value("${keycloak.cv-service.client_id}")
    private String clientId;

    @Value("${keycloak.cv-service.client_secret}")
    private String clientSecret;

    @Value("${keycloak.cv-service.urls.token}")
    private String tokenUrl;

    public String getServiceToken() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(formData, headers);

        try {
            Map<String, String> response = new RestTemplate().postForEntity(
                    tokenUrl,
                    httpEntity,
                    Map.class
            ).getBody();

            if (response == null) {
                throw new ServiceTokenException("Token retrieval error: response is null");
            }

            return response.get("access_token");
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("Keycloak error during: {}", ex.getResponseBodyAsString());
            throw new ServiceTokenException("Token retrieval error: " + ex.getMessage());
        }
    }
}
