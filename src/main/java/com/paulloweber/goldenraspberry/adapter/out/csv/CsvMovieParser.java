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
import java.util.stream.Collectors;

/**
 * Reads the semicolon-separated movie list. Every row is validated before any
 * movie is returned, so a single bad line rejects the whole file.
 */
@Component
public class CsvMovieParser implements MovieParserPort {
    private static final String EXPECTED_HEADER = "year;title;studios;producers;winner";
    private static final int MAX_REPORTED_ERRORS = 20;
    private static final char SEPARATOR = ';';
    private static final char QUOTE = '"';

    /** Splits "A, B", "A and B" and "A, B, and C" alike. */
    private static final Pattern PRODUCER_SEPARATOR = Pattern.compile(",\\s*and\\s+|,\\s*|\\s+and\\s+");

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
        String stripped = header == null ? "" : header.replace("\uFEFF", "").trim();
        List<String> columns = splitRow(stripped);
        String normalized = columns == null ? "" : columns.stream()
                .map(column -> column.trim().toLowerCase())
                .collect(Collectors.joining(";"));
        if (!EXPECTED_HEADER.equals(normalized)) {
            throw new InvalidMovieDataException(List.of(
                    "First line must be the header \"" + EXPECTED_HEADER + "\""));
        }
    }

    private void parseMovie(String line, int lineNumber, List<Movie> movies, List<String> errors) {
        List<String> fields = splitRow(line);
        if (fields == null) {
            errors.add("Line " + lineNumber + ": unterminated quoted field");
            return;
        }
        if (fields.size() < 4) {
            errors.add("Line " + lineNumber + ": expected at least 4 fields separated by ';'");
            return;
        }

        int year;
        try {
            year = Integer.parseInt(fields.get(0).trim());
        } catch (NumberFormatException e) {
            errors.add("Line " + lineNumber + ": year \"" + fields.get(0).trim() + "\" is not a number");
            return;
        }

        String title = fields.get(1).trim();
        if (title.isEmpty()) {
            errors.add("Line " + lineNumber + ": title must not be empty");
            return;
        }

        boolean winner = fields.size() > 4 && "yes".equalsIgnoreCase(fields.get(4).trim());
        movies.add(new Movie(year, title, fields.get(2).trim(), winner, producersOf(fields.get(3))));
    }

    /**
     * Splits one row on {@code ;}, honouring RFC 4180 quoting: a field wrapped in double
     * quotes may contain the separator, and {@code ""} inside it is a literal quote.
     *
     * <p>Plain rows \u2014 the shape of the provided file \u2014 take the same path and are unaffected.
     *
     * @return the fields, or {@code null} when a quoted field is never closed. Reporting that
     *         as an error beats splitting on the separators inside it, which would silently
     *         shift every later column by one.
     */
    private List<String> splitRow(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c != QUOTE) {
                    field.append(c);
                } else if (i + 1 < line.length() && line.charAt(i + 1) == QUOTE) {
                    field.append(QUOTE);
                    i++;
                } else {
                    quoted = false;
                }
            } else if (c == QUOTE && field.isEmpty()) {
                quoted = true;
            } else if (c == SEPARATOR) {
                fields.add(field.toString());
                field.setLength(0);
            } else {
                field.append(c);
            }
        }

        if (quoted) {
            return null;
        }
        fields.add(field.toString());
        return fields;
    }

    private List<Producer> producersOf(String field) {
        return PRODUCER_SEPARATOR.splitAsStream(field)
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .map(Producer::new)
                .toList();
    }
}
