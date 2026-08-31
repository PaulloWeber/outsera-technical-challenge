package com.paulloweber.goldenraspberry.adapter.out.persistence;

import com.paulloweber.goldenraspberry.adapter.out.persistence.entity.MovieEntity;
import com.paulloweber.goldenraspberry.adapter.out.persistence.entity.ProducerEntity;
import com.paulloweber.goldenraspberry.application.port.out.MovieRepositoryPort;
import com.paulloweber.goldenraspberry.domain.model.Movie;
import com.paulloweber.goldenraspberry.domain.model.Producer;
import com.paulloweber.goldenraspberry.domain.model.ProducerWin;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Translates between the domain and JPA. The domain never sees an entity,
 * and the entities never leak past this class.
 */
@Component
public class MoviePersistenceAdapter implements MovieRepositoryPort {
    private final MovieJpaRepository movieJpaRepository;
    private final ProducerJpaRepository producerJpaRepository;

    public MoviePersistenceAdapter(MovieJpaRepository movieJpaRepository,
                                   ProducerJpaRepository producerJpaRepository) {
        this.movieJpaRepository = movieJpaRepository;
        this.producerJpaRepository = producerJpaRepository;
    }

    @Override
    public void replaceAll(List<Movie> movies) {
        movieJpaRepository.deleteAll();
        movieJpaRepository.flush();
        producerJpaRepository.deleteAllInBatch();
        movieJpaRepository.saveAll(toEntities(movies));
    }

    @Override
    public List<ProducerWin> findAllWins() {
        return movieJpaRepository.findAllWins().stream()
                .map(win -> new ProducerWin(win.getProducer(), win.getYear()))
                .toList();
    }

    /** One {@link ProducerEntity} per distinct name, shared across every movie that credits them. */
    private List<MovieEntity> toEntities(List<Movie> movies) {
        Map<String, ProducerEntity> producersByName = new HashMap<>();
        return movies.stream().map(movie -> {
            MovieEntity entity = new MovieEntity(movie.year(), movie.title(), movie.studios(), movie.winner());
            entity.addProducers(movie.producers().stream()
                    .map(Producer::name)
                    .map(name -> producersByName.computeIfAbsent(name, ProducerEntity::new))
                    .toList());
            return entity;
        }).toList();
    }
}
