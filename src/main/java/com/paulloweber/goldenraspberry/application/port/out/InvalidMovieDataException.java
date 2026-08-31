package com.paulloweber.goldenraspberry.application.port.out;

import java.util.List;

/**
 * The source data could not be read. Carries every problem found, not just the first,
 * so the caller can report them all at once.
 */
public class InvalidMovieDataException extends RuntimeException {
    private final List<String> errors;

    public InvalidMovieDataException(List<String> errors) {
        super("Invalid movie data: " + String.join("; ", errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}
