package com.paulloweber.goldenraspberry.infrastructure.bootstrap;

import com.paulloweber.goldenraspberry.application.port.in.ImportMoviesUseCase;
import com.paulloweber.goldenraspberry.application.port.in.ImportResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * Loads the movie list into the database on startup, satisfying requirement 2.1.
 * Runs once every bean exists, so the repositories are ready.
 */
@Component
public class MovieCsvLoader implements SmartInitializingSingleton {
    private static final Logger log = LoggerFactory.getLogger(MovieCsvLoader.class);

    private final ResourceLoader resourceLoader;
    private final ImportMoviesUseCase importMovies;
    private final String csvLocation;

    public MovieCsvLoader(ResourceLoader resourceLoader,
                          ImportMoviesUseCase importMovies,
                          @Value("${app.movies-csv:classpath:Movielist.csv}") String csvLocation) {
        this.resourceLoader = resourceLoader;
        this.importMovies = importMovies;
        this.csvLocation = csvLocation;
    }

    @Override
    public void afterSingletonsInstantiated() {
        Resource csv = resourceLoader.getResource(csvLocation);
        try (InputStream input = csv.getInputStream()) {
            ImportResult result = importMovies.importMovies(input);
            log.info("Loaded {} movies ({} winners, {} distinct producers) from {}",
                    result.movies(), result.winners(), result.producers(), csvLocation);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read movie CSV at " + csvLocation, e);
        }
    }
}
