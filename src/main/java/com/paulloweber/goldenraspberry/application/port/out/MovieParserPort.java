package com.paulloweber.goldenraspberry.application.port.out;

import com.paulloweber.goldenraspberry.domain.model.Movie;

import java.io.InputStream;
import java.util.List;

/** Driven port: turns a raw movie list into domain objects. */
public interface MovieParserPort {

    /**
     * Reads every movie from the stream.
     *
     * @throws InvalidMovieDataException if the source is malformed; nothing is returned partially parsed
     */
    List<Movie> parse(InputStream input);
}
