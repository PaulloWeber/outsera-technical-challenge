package com.paulloweber.goldenraspberry.adapter.out.persistence;

import com.paulloweber.goldenraspberry.adapter.out.persistence.entity.MovieEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MovieJpaRepository extends JpaRepository<MovieEntity, Long> {

    @Query("""
            select p.name as producer, m.year as year
            from MovieEntity m
            join m.producers p
            where m.winner = true
            """)
    List<ProducerWinView> findAllWins();
}
