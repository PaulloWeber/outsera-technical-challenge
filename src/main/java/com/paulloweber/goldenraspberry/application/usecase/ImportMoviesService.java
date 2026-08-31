package com.paulloweber.goldenraspberry.application.usecase;

import com.paulloweber.goldenraspberry.application.port.in.ImportMoviesUseCase;
import com.paulloweber.goldenraspberry.application.port.in.ImportResult;
import com.paulloweber.goldenraspberry.application.port.out.MovieParserPort;
import com.paulloweber.goldenraspberry.application.port.out.MovieRepositoryPort;
import com.paulloweber.goldenraspberry.domain.model.Movie;
import com.paulloweber.goldenraspberry.domain.model.Producer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;

@Service
public class ImportMoviesService implements ImportMoviesUseCase {
    private final MovieParserPort movieParser;
    private final MovieRepositoryPort movieRepository;

    public ImportMoviesService(MovieParserPort movieParser, MovieRepositoryPort movieRepository) {
        this.movieParser = movieParser;
        this.movieRepository = movieRepository;
    }

    /**
     * The source is fully parsed and validated before anything is written, so a
     * malformed file leaves the current dataset untouched.
     */
    @Override
    @Transactional
    public ImportResult importMovies(InputStream source) {
        List<Movie> movies = movieParser.parse(source);
        movieRepository.replaceAll(movies);
        return summarise(movies);
    }

    private ImportResult summarise(List<Movie> movies) {
        int winners = (int) movies.stream().filter(Movie::winner).count();
        int producers = (int) movies.stream()
                .flatMap(movie -> movie.producers().stream())
                .map(Producer::name)
                .distinct()
                .count();
        return new ImportResult(movies.size(), winners, producers);
    }
}
