package com.paulloweber.goldenraspberry.domain.service;

import com.paulloweber.goldenraspberry.domain.model.AwardInterval;
import com.paulloweber.goldenraspberry.domain.model.AwardIntervals;
import com.paulloweber.goldenraspberry.domain.model.ProducerWin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * The single business rule of this application: given every award won, find the
 * producers who waited the longest between two consecutive wins and the ones who
 * won twice the fastest.
 *
 * <p>Deliberately free of framework, persistence and HTTP concerns, so it can be
 * exercised with nothing but a list of wins.
 */
public class AwardIntervalCalculator {

    /** Sorting by producer then year puts every pair of consecutive wins side by side. */
    private static final Comparator<ProducerWin> BY_PRODUCER_THEN_YEAR =
            Comparator.comparing(ProducerWin::producer).thenComparingInt(ProducerWin::year);

    /** Presentation order for tied producers: oldest streak first, then alphabetically. */
    private static final Comparator<AwardInterval> BY_OLDEST_WIN =
            Comparator.comparingInt(AwardInterval::previousWin).thenComparing(AwardInterval::producer);

    /**
     * Two passes over the wins and one over the intervals they produce.
     *
     * <p>Sorting once by {@code (producer, year)} makes neighbouring entries that share a
     * producer exactly the consecutive wins we are looking for, so a single scan builds
     * every interval and tracks both extremes at the same time. A producer who won only
     * once never has a neighbour of their own and drops out on its own.
     */
    public AwardIntervals calculate(Collection<ProducerWin> wins) {
        List<ProducerWin> ordered = new ArrayList<>(wins);
        ordered.sort(BY_PRODUCER_THEN_YEAR);

        List<AwardInterval> intervals = new ArrayList<>();
        int shortest = Integer.MAX_VALUE;
        int longest = Integer.MIN_VALUE;

        for (int i = 1; i < ordered.size(); i++) {
            ProducerWin earlier = ordered.get(i - 1);
            ProducerWin later = ordered.get(i);
            if (!earlier.producer().equals(later.producer())) {
                continue;
            }
            int gap = later.year() - earlier.year();
            intervals.add(new AwardInterval(later.producer(), gap, earlier.year(), later.year()));
            shortest = Math.min(shortest, gap);
            longest = Math.max(longest, gap);
        }

        if (intervals.isEmpty()) {
            return AwardIntervals.empty();
        }

        List<AwardInterval> min = new ArrayList<>();
        List<AwardInterval> max = new ArrayList<>();
        for (AwardInterval interval : intervals) {
            if (interval.interval() == shortest) {
                min.add(interval);
            }
            if (interval.interval() == longest) {
                max.add(interval);
            }
        }

        min.sort(BY_OLDEST_WIN);
        max.sort(BY_OLDEST_WIN);
        return new AwardIntervals(min, max);
    }
}
