package com.paulloweber.goldenraspberry.domain.model;

import java.util.List;

/**
 * The shortest and the longest intervals between consecutive awards.
 * Both lists hold every producer tied on that interval, and are empty
 * when no producer has won more than once.
 */
public record AwardIntervals(List<AwardInterval> min, List<AwardInterval> max) {
    public AwardIntervals {
        min = List.copyOf(min);
        max = List.copyOf(max);
    }

    public static AwardIntervals empty() {
        return new AwardIntervals(List.of(), List.of());
    }
}
