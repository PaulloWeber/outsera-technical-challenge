package com.paulloweber.goldenraspberry.application.port.out;

import com.paulloweber.goldenraspberry.domain.model.Movie;
import com.paulloweber.goldenraspberry.domain.model.ProducerWin;

import java.util.List;

/** Driven port: where movies are stored and how wins are read back. */
public interface MovieRepositoryPort {

    /** Atomically discards the current dataset and stores the given one. */
    void replaceAll(List<Movie> movies);

    /** Every award won, one entry per producer per winning movie. */
    List<ProducerWin> findAllWins();
}
