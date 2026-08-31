package com.paulloweber.goldenraspberry.domain.model;

import java.util.Collection;
import java.util.Set;

/**
 * A movie nominated for the Worst Picture category, and whether it won.
 */
public record Movie(int year, String title, String studios, boolean winner, Set<Producer> producers) {
    public Movie {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Movie title must not be blank");
        }
        producers = producers == null ? Set.of() : Set.copyOf(producers);
    }

    public Movie(int year, String title, String studios, boolean winner, Collection<Producer> producers) {
        this(year, title, studios, winner, Set.copyOf(producers));
    }
}
