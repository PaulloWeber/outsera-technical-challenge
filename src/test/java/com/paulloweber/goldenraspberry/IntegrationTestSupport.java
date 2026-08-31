package com.paulloweber.goldenraspberry;

import com.paulloweber.goldenraspberry.adapter.in.web.AuthController.TokenResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots the whole application on a random port and talks to it over real HTTP.
 * Nothing is mocked: the tests exercise the same wiring the deployed app uses.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
abstract class IntegrationTestSupport {
    protected static final String OPEN_INTERVALS_ROUTE = "/api/public/producers/win-intervals";
    protected static final String PROTECTED_INTERVALS_ROUTE = "/api/producers/win-intervals";
    protected static final String IMPORT_ROUTE = "/api/movies/import";

    @Autowired
    protected TestRestTemplate restTemplate;

    protected String userToken() {
        return tokenFor("user", "passUser");
    }

    protected String adminToken() {
        return tokenFor("admin", "passAdmin");
    }

    protected String tokenFor(String username, String password) {
        ResponseEntity<TokenResponse> response = restTemplate
                .withBasicAuth(username, password)
                .postForEntity("/auth/token", null, TokenResponse.class);
        assertThat(response.getStatusCode().is2xxSuccessful())
                .as("token request for %s", username)
                .isTrue();
        return response.getBody().token();
    }

    protected HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    protected <T> ResponseEntity<T> getAuthorized(String url, String token, Class<T> responseType) {
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(bearerHeaders(token)), responseType);
    }

    /** Posts a file to the import endpoint. A {@code null} token sends no credentials at all. */
    protected ResponseEntity<String> postFile(String token, String filename, String content) {
        ByteArrayResource attachment = new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", attachment);

        HttpHeaders headers = token == null ? new HttpHeaders() : bearerHeaders(token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return restTemplate.postForEntity(IMPORT_ROUTE, new HttpEntity<>(form, headers), String.class);
    }
}
