package com.paulloweber.goldenraspberry;

import com.paulloweber.goldenraspberry.adapter.in.web.dto.AwardIntervalsResponse;
import com.paulloweber.goldenraspberry.adapter.in.web.dto.ProducerAwardInterval;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class AwardIntervalsIntegrationTest extends IntegrationTestSupport {

    @Test
    void exposesTheMinAndMaxFieldsRequiredBySpecification() {
        ResponseEntity<Map<String, List<Map<String, Object>>>> response = restTemplate.exchange(
                OPEN_INTERVALS_ROUTE, HttpMethod.GET, null, new ParameterizedTypeReference<>() {
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType())
                .isNotNull()
                .matches(type -> type.isCompatibleWith(MediaType.APPLICATION_JSON));

        // Checking the parsed shape rather than searching the raw text: this also proves
        // there are no extra fields beyond the four the specification asks for.
        assertThat(response.getBody()).containsOnlyKeys("min", "max");
        assertThat(response.getBody().values()).allSatisfy(entries ->
                assertThat(entries).isNotEmpty().allSatisfy(entry ->
                        assertThat(entry).containsOnlyKeys(
                                "producer", "interval", "previousWin", "followingWin")));
    }

    @Test
    void resolvesShortestAndLongestGapsFromTheBundledDataset() {
        AwardIntervalsResponse body =
                restTemplate.getForObject(OPEN_INTERVALS_ROUTE, AwardIntervalsResponse.class);

        assertThat(body.min()).containsExactly(
                new ProducerAwardInterval("Joel Silver", 1, 1990, 1991));
        assertThat(body.max()).containsExactly(
                new ProducerAwardInterval("Matthew Vaughn", 13, 2002, 2015));
    }

    /**
     * Cross-checks the API against a second, deliberately different implementation of the
     * same rule, computed straight from the source file. If the production calculation
     * ever drifts, this fails even when the hard-coded expectations above were updated
     * to match the drift.
     */
    @Test
    void agreesWithAnIndependentRecalculationOfTheSourceFile() throws IOException {
        List<ProducerAwardInterval> expected = recalculateFromSource("/Movielist.csv");
        int shortest = expected.stream().mapToInt(ProducerAwardInterval::interval).min().orElseThrow();
        int longest = expected.stream().mapToInt(ProducerAwardInterval::interval).max().orElseThrow();

        AwardIntervalsResponse body =
                restTemplate.getForObject(OPEN_INTERVALS_ROUTE, AwardIntervalsResponse.class);

        assertThat(body.min()).containsExactlyInAnyOrderElementsOf(withInterval(expected, shortest));
        assertThat(body.max()).containsExactlyInAnyOrderElementsOf(withInterval(expected, longest));
    }

    private List<ProducerAwardInterval> withInterval(List<ProducerAwardInterval> all, int interval) {
        return all.stream().filter(entry -> entry.interval() == interval).toList();
    }

    /**
     * Buckets the wins by producer and pairs the years inside each bucket — a different
     * route to the same answer than the single sorted scan the application performs.
     *
     * <p>Keeping the two algorithms apart is the point: if both sides used the same one,
     * this test would only prove the code agrees with itself.
     */
    private List<ProducerAwardInterval> recalculateFromSource(String classpathLocation) throws IOException {
        Map<String, List<Integer>> yearsByProducer = new LinkedHashMap<>();
        for (Win win : readWins(classpathLocation)) {
            yearsByProducer.computeIfAbsent(win.producer(), producer -> new ArrayList<>()).add(win.year());
        }

        List<ProducerAwardInterval> intervals = new ArrayList<>();
        yearsByProducer.forEach((producer, years) -> {
            List<Integer> sorted = years.stream().sorted().toList();
            for (int i = 1; i < sorted.size(); i++) {
                intervals.add(new ProducerAwardInterval(
                        producer, sorted.get(i) - sorted.get(i - 1), sorted.get(i - 1), sorted.get(i)));
            }
        });
        return intervals;
    }

    private static final Pattern CREDIT_SEPARATOR = Pattern.compile(",\\s*and\\s+|,\\s*|\\s+and\\s+");

    private List<Win> readWins(String classpathLocation) throws IOException {
        List<Win> wins = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream(classpathLocation)),
                StandardCharsets.UTF_8))) {
            reader.readLine();
            reader.lines()
                    .filter(line -> !line.isBlank())
                    .map(line -> line.split(";", -1))
                    .filter(columns -> columns.length >= 5 && "yes".equalsIgnoreCase(columns[4].trim()))
                    .forEach(columns -> {
                        int year = Integer.parseInt(columns[0].trim());
                        CREDIT_SEPARATOR.splitAsStream(columns[3])
                                .map(String::trim)
                                .filter(name -> !name.isEmpty())
                                .forEach(name -> wins.add(new Win(name, year)));
                    });
        }
        return wins;
    }

    private record Win(String producer, int year) {
    }
}
