package com.paulloweber.goldenraspberry.adapter.out.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "movies")
public class MovieEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "release_year", nullable = false)
    private int year;

    @Column(nullable = false)
    private String title;

    private String studios;

    @Column(nullable = false)
    private boolean winner;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "movie_producers",
            joinColumns = @JoinColumn(name = "movie_id"),
            inverseJoinColumns = @JoinColumn(name = "producer_id"))
    private Set<ProducerEntity> producers = new LinkedHashSet<>();

    protected MovieEntity() {
    }

    public MovieEntity(int year, String title, String studios, boolean winner) {
        this.year = year;
        this.title = title;
        this.studios = studios;
        this.winner = winner;
    }

    public void addProducers(Collection<ProducerEntity> toAdd) {
        producers.addAll(toAdd);
    }

    public Long getId() {
        return id;
    }

    public int getYear() {
        return year;
    }

    public String getTitle() {
        return title;
    }

    public String getStudios() {
        return studios;
    }

    public boolean isWinner() {
        return winner;
    }

    public Set<ProducerEntity> getProducers() {
        return producers;
    }
}
