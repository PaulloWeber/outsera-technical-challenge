package com.paulloweber.goldenraspberry;

import com.paulloweber.goldenraspberry.adapter.in.web.dto.AwardIntervalsResponse;
import com.paulloweber.goldenraspberry.adapter.in.web.dto.ProducerAwardInterval;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import java.util.stream.Stream;

import static com.paulloweber.goldenraspberry.MovieCsvFixture.movieList;
import static org.assertj.core.api.Assertions.assertThat;

class CsvUploadIntegrationTest extends IntegrationTestSupport {

    @Test
    @DirtiesContext
    void swapsTheActiveDatasetWhenAWellFormedFileArrives() {
        String csv = movieList()
                .winner(2000, "Movie A", "Producer X")
                .winner(2003, "Movie B", "Producer X")
                .winner(2004, "Movie C", "Producer Y", "Producer X")
                .winner(2010, "Movie D", "Producer Y")
                .nominee(2011, "Movie E", "Producer Z")
                .render();

        ResponseEntity<String> upload = uploadAsAdmin("movies.csv", csv);

        assertThat(upload.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(upload.getBody())
                .contains("\"movies\":5")
                .contains("\"winners\":4")
                .contains("\"producers\":3");

        AwardIntervalsResponse intervals = currentIntervals();
        assertThat(intervals.min()).containsExactly(
                new ProducerAwardInterval("Producer X", 1, 2003, 2004));
        assertThat(intervals.max()).containsExactly(
                new ProducerAwardInterval("Producer Y", 6, 2004, 2010));
    }

    @Test
    @DirtiesContext
    void listsAllProducersSharingTheShortestAndLongestGap() {
        // A and B both wait a single year; C and D both wait ten. Every one of the four
        // has to come back, ordered by the year the streak started.
        String csv = movieList()
                .winner(2000, "Film A1", "Producer A").winner(2001, "Film A2", "Producer A")
                .winner(2005, "Film B1", "Producer B").winner(2006, "Film B2", "Producer B")
                .winner(2003, "Film D1", "Producer D").winner(2013, "Film D2", "Producer D")
                .winner(2010, "Film C1", "Producer C").winner(2020, "Film C2", "Producer C")
                .render();

        assertThat(uploadAsAdmin("ties.csv", csv).getStatusCode()).isEqualTo(HttpStatus.OK);

        AwardIntervalsResponse intervals = currentIntervals();
        assertThat(intervals.min()).containsExactly(
                new ProducerAwardInterval("Producer A", 1, 2000, 2001),
                new ProducerAwardInterval("Producer B", 1, 2005, 2006));
        assertThat(intervals.max()).containsExactly(
                new ProducerAwardInterval("Producer D", 10, 2003, 2013),
                new ProducerAwardInterval("Producer C", 10, 2010, 2020));
    }

    @Test
    @DirtiesContext
    void answersWithEmptyListsWhenNobodyRepeats() {
        String csv = movieList()
                .winner(1980, "Solo One", "Alice")
                .winner(1985, "Solo Two", "Bob")
                .nominee(1990, "Never Won", "Carol")
                .render();

        assertThat(uploadAsAdmin("singles.csv", csv).getStatusCode()).isEqualTo(HttpStatus.OK);

        AwardIntervalsResponse intervals = currentIntervals();
        assertThat(intervals.min()).isEmpty();
        assertThat(intervals.max()).isEmpty();
    }

    /**
     * Every rejection has to behave the same way: 400, an explanation naming the problem,
     * and the previously loaded dataset left exactly as it was.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("rejectedUploads")
    void refusesBadInputWithoutDisturbingTheLoadedDataset(RejectedUpload scenario) {
        ResponseEntity<String> upload = uploadAsAdmin(scenario.filename(), scenario.content());

        assertThat(upload.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(upload.getBody()).contains(scenario.explanation());

        assertBundledDatasetStillServed();
    }

    static Stream<RejectedUpload> rejectedUploads() {
        return Stream.of(
                new RejectedUpload("header in another language", "movies.csv",
                        movieList().headedBy("ano;titulo;estudios;produtores;vencedor")
                                .winner(2000, "Movie A", "Producer X")
                                .render(),
                        MovieCsvFixture.CANONICAL_HEADER),
                new RejectedUpload("year that is not a number", "movies.csv",
                        movieList().winner(2000, "Movie A", "Producer X")
                                .verbatimRow("20XX;Movie B;Studio 1;Producer X;yes")
                                .render(),
                        "Line 3"),
                new RejectedUpload("attachment with another extension", "movies.txt",
                        "not a csv",
                        "Only .csv files are accepted"),
                new RejectedUpload("attachment with no content", "movies.csv",
                        "",
                        "empty"));
    }

    record RejectedUpload(String label, String filename, String content, String explanation) {
        @Override
        public String toString() {
            return label;
        }
    }

    private void assertBundledDatasetStillServed() {
        AwardIntervalsResponse intervals = currentIntervals();
        assertThat(intervals.min()).containsExactly(
                new ProducerAwardInterval("Joel Silver", 1, 1990, 1991));
        assertThat(intervals.max()).containsExactly(
                new ProducerAwardInterval("Matthew Vaughn", 13, 2002, 2015));
    }

    private AwardIntervalsResponse currentIntervals() {
        return restTemplate.getForObject(OPEN_INTERVALS_ROUTE, AwardIntervalsResponse.class);
    }

    private ResponseEntity<String> uploadAsAdmin(String filename, String content) {
        return postFile(adminToken(), filename, content);
    }
}
