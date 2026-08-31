package com.paulloweber.goldenraspberry;

import com.paulloweber.goldenraspberry.adapter.in.web.AuthController.TokenResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static com.paulloweber.goldenraspberry.MovieCsvFixture.movieList;
import static org.assertj.core.api.Assertions.assertThat;

class SecurityIntegrationTest extends IntegrationTestSupport {

    @Test
    void mintsAJwtForKnownCredentials() {
        ResponseEntity<TokenResponse> response = restTemplate
                .withBasicAuth("user", "passUser")
                .postForEntity("/auth/token", null, TokenResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().token()).isNotBlank();
        assertThat(response.getBody().tokenType()).isEqualTo("Bearer");
        assertThat(response.getBody().expiresIn()).isEqualTo(3600);
    }

    @Test
    void deniesAJwtWhenThePasswordIsWrong() {
        ResponseEntity<String> response = restTemplate
                .withBasicAuth("user", "wrong-password")
                .postForEntity("/auth/token", null, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /** A protected read needs a credential the application itself issued — nothing else passes. */
    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "no credential at all,",
            "a token this application never signed, not-a-real-token"
    })
    void answersUnauthorizedOnTheProtectedRouteWithout(String label, String token) {
        ResponseEntity<String> response = token == null
                ? restTemplate.getForEntity(PROTECTED_INTERVALS_ROUTE, String.class)
                : getAuthorized(PROTECTED_INTERVALS_ROUTE, token, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void servesTheProtectedRouteToAnyValidToken() {
        ResponseEntity<String> response = getAuthorized(PROTECTED_INTERVALS_ROUTE, userToken(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void returnsAnIdenticalPayloadOnTheOpenAndProtectedRoutes() {
        ResponseEntity<String> open = restTemplate.getForEntity(OPEN_INTERVALS_ROUTE, String.class);
        ResponseEntity<String> protectedRoute =
                getAuthorized(PROTECTED_INTERVALS_ROUTE, userToken(), String.class);

        assertThat(open.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(open.getBody()).isEqualTo(protectedRoute.getBody());
    }

    @Test
    void answersUnauthorizedWhenImportingWithoutAToken() {
        assertThat(importAttemptBy(null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void answersForbiddenWhenTheCallerLacksAdmin() {
        assertThat(importAttemptBy(userToken()).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    /** The payload is valid on purpose: only the credential should decide the outcome. */
    private ResponseEntity<String> importAttemptBy(String token) {
        String csv = movieList().winner(2000, "Movie", "Producer").render();
        return postFile(token, "movies.csv", csv);
    }
}
