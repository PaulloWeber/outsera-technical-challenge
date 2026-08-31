package com.paulloweber.goldenraspberry.adapter.out.csv;

import com.paulloweber.goldenraspberry.application.port.out.InvalidMovieDataException;
import com.paulloweber.goldenraspberry.application.port.out.MovieParserPort;
import com.paulloweber.goldenraspberry.domain.model.Movie;
import com.paulloweber.goldenraspberry.domain.model.Producer;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Reads the semicolon-separated movie list. Every row is validated before any
 * movie is returned, so a single bad line rejects the whole file.
 */
@Component
public class CsvMovieParser implements MovieParserPort {
    private static final String EXPECTED_HEADER = "year;title;studios;producers;winner";
    private static final int MAX_REPORTED_ERRORS = 20;

    /** Splits "A, B", "A and B" and "A, B, and C" alike. */
    private static final Pattern PRODUCER_SEPARATOR = Pattern.compile(",\s*and\s+|,\s*|\s+and\s+");

    @Override
    public List<Movie> parse(InputStream input) {
        List<Movie> movies = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            validateHeader(reader.readLine());

            int lineNumber = 1;
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                parseMovie(line, lineNumber, movies, errors);
                if (errors.size() >= MAX_REPORTED_ERRORS) {
                    errors.add("Too many errors, aborting");
                    break;
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read CSV content", e);
        }

        if (!errors.isEmpty()) {
            throw new InvalidMovieDataException(errors);
        }
        return movies;
    }

    private void validateHeader(String header) {
        String normalized = header == null ? "" : header.replace("\uFEFF", "").trim().toLowerCase();
        if (!EXPECTED_HEADER.equals(normalized)) {
            throw new InvalidMovieDataException(List.of(
                    "First line must be the header \"" + EXPECTED_HEADER + "\""));
        }
    }

    private void parseMovie(String line, int lineNumber, List<Movie> movies, List<String> errors) {
        String[] fields = line.split(";", -1);
        if (fields.length < 4) {
            errors.add("Line " + lineNumber + ": expected at least 4 fields separated by ';'");
            return;
        }

        int year;
        try {
            year = Integer.parseInt(fields[0].trim());
        } catch (NumberFormatException e) {
            errors.add("Line " + lineNumber + ": year \"" + fields[0].trim() + "\" is not a number");
            return;
        }

        String title = fields[1].trim();
        if (title.isEmpty()) {
            errors.add("Line " + lineNumber + ": title must not be empty");
            return;
        }

        boolean winner = fields.length > 4 && "yes".equalsIgnoreCase(fields[4].trim());
        movies.add(new Movie(year, title, fields[2].trim(), winner, producersOf(fields[3])));
    }

    private List<Producer> producersOf(String field) {
        return PRODUCER_SEPARATOR.splitAsStream(field)
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .map(Producer::new)
                .toList();
    }
}
