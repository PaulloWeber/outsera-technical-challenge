package com.paulloweber.goldenraspberry.adapter.out.persistence;

import com.paulloweber.goldenraspberry.adapter.out.persistence.entity.MovieEntity;
import com.paulloweber.goldenraspberry.domain.model.ProducerWin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MovieJpaRepository extends JpaRepository<MovieEntity, Long> {

    /**
     * Every award won, one row per producer per winning movie. The constructor expression
     * materialises the domain record straight from the result set, so no intermediate
     * projection has to be walked and copied afterwards.
     */
    @Query("""
            select new com.paulloweber.goldenraspberry.domain.model.ProducerWin(p.name, m.year)
            from MovieEntity m
            join m.producers p
            where m.winner = true
            """)
    List<ProducerWin> findAllWins();
}
