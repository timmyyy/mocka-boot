package ru.ned.mockboot;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.HttpHeaders.COOKIE;
import static org.springframework.http.HttpHeaders.SET_COOKIE;

@SpringBootApplication
@RestController
public class MockaBootApplication {
    private static Logger log = LoggerFactory.getLogger(MockaBootApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(MockaBootApplication.class, args);
    }

    @Autowired
    @Qualifier("appRestClient")
    private RestTemplate appRestClient;

    @RequestMapping("/register")
    public String register() {
        log.debug("/register ==>");
        ResponseEntity<String> redirectResponse = appRestClient.exchange(
                "/extSystem/redirect",
                HttpMethod.GET,
                null,
                String.class);

        String cookie = redirectResponse.getHeaders().getFirst(SET_COOKIE);

        HttpHeaders headers = new HttpHeaders();
        headers.add(COOKIE, cookie);

        ParameterizedTypeReference<dataInfo> responseType =
                new ParameterizedTypeReference<dataInfo>() {
                };

        ResponseEntity<dataInfo> cbResponse = appRestClient.exchange(
                "/extSystem/foo?status=sGOxKJxiXbs4&code=365FC2E9-CAFE-BABE-FBD8-D65380BB6493",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                responseType);

        String userId = cbResponse.getBody().getData().get("userId");

        Map<String, String> paramsWithuserId = new HashMap<>();
        paramsWithuserId.put("userId", userId);

        ResponseEntity<String> resendSmsResponse = appRestClient.exchange(
                "/api/v2/users/{userId}/resend_sms",
                HttpMethod.POST,
                null,
                String.class,
                paramsWithuserId);

        ResponseEntity<String> smsResponse = appRestClient.exchange(
                "/api/v2/users/{userId}/sms?code=3044",
                HttpMethod.PUT,
                null,
                String.class,
                paramsWithuserId);

        ResponseEntity<String> generatePasswordResponse = appRestClient.exchange(
                "/api/v2/users/{userId}/generate_password",
                HttpMethod.PATCH,
                null,
                String.class,
                paramsWithuserId);

        log.debug("/register <==");
        return generatePasswordResponse.getBody();
    }
}

class dataInfo {
    private Map<String, String> data;

    public Map<String, String> getData() {
        return data;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }
}

@Configuration
class appClient {
    @Value("${app.host}")
    private String appHost;

    @Bean
    public RestTemplate appRestClient() {

        RestTemplate restTemplate = new RestTemplateBuilder()
                .requestFactory(HttpComponentsClientHttpRequestFactory.class)
                .rootUri(appHost)
                .interceptors(new RequestResponseLoggingInterceptor())
                .build();

        HttpClient httpClient = HttpClientBuilder.create().setRedirectStrategy(new NoRedirectStrategy()).build();
        HttpComponentsClientHttpRequestFactory reqFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        restTemplate.setRequestFactory(reqFactory);

        return restTemplate;
    }
}

class NoRedirectStrategy extends DefaultRedirectStrategy {
    @Override
    public boolean isRedirected(HttpRequest request, HttpResponse response, HttpContext context) throws ProtocolException {
        return false;
    }
}