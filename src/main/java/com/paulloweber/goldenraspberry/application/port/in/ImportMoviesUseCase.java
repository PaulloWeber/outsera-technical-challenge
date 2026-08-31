package com.paulloweber.goldenraspberry.application.port.in;

import java.io.InputStream;

/** Driving port: replace the whole dataset with the movies read from a source. */
public interface ImportMoviesUseCase {
    ImportResult importMovies(InputStream source);
}
