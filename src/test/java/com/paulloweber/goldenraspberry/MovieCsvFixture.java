package com.paulloweber.goldenraspberry;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Builds movie lists for the upload tests.
 *
 * <p>Spelling a scenario out as {@code winner(2000, "Movie A", "Producer X")} keeps the
 * intent of each row visible, and lets the tests exercise the three separator styles the
 * parser has to cope with without hand-writing them: a lone name, "A and B", and
 * "A, B and C".
 */
final class MovieCsvFixture {
    static final String CANONICAL_HEADER = "year;title;studios;producers;winner";

    private final List<String> rows = new ArrayList<>();
    private String header = CANONICAL_HEADER;
    private String studios = "Some Studio";

    private MovieCsvFixture() {
    }

    static MovieCsvFixture movieList() {
        return new MovieCsvFixture();
    }

    MovieCsvFixture headedBy(String replacement) {
        this.header = replacement;
        return this;
    }

    MovieCsvFixture releasedBy(String studio) {
        this.studios = studio;
        return this;
    }

    MovieCsvFixture winner(int year, String title, String... producers) {
        return row(year, title, "yes", producers);
    }

    MovieCsvFixture nominee(int year, String title, String... producers) {
        return row(year, title, "", producers);
    }

    /** Appends a row exactly as given, for malformed input the builder would not produce. */
    MovieCsvFixture verbatimRow(String row) {
        rows.add(row);
        return this;
    }

    private MovieCsvFixture row(int year, String title, String won, String... producers) {
        rows.add(String.join(";", String.valueOf(year), title, studios, credit(producers), won));
        return this;
    }

    /** "A" · "A and B" · "A, B and C" — the three shapes found in the real file. */
    private String credit(String... producers) {
        if (producers.length <= 1) {
            return producers.length == 0 ? "" : producers[0];
        }
        String allButLast = String.join(", ", List.of(producers).subList(0, producers.length - 1));
        return allButLast + " and " + producers[producers.length - 1];
    }

    String render() {
        return Stream.concat(Stream.of(header), rows.stream())
                .collect(Collectors.joining("\n", "", "\n"));
    }
}
