package com.paulloweber.goldenraspberry.domain.model;

/**
 * A producer, identified by name exactly as it appears in the source data.
 * Names are never normalised: "Michael DeLuca" and "Michael De Luca" are different producers.
 */
public record Producer(String name) {
    public Producer {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Producer name must not be blank");
        }
        name = name.trim();
    }
}
