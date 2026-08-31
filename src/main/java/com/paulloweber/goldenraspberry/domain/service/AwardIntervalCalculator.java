package com.paulloweber.goldenraspberry.domain.service;

import com.paulloweber.goldenraspberry.domain.model.AwardInterval;
import com.paulloweber.goldenraspberry.domain.model.AwardIntervals;
import com.paulloweber.goldenraspberry.domain.model.ProducerWin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The single business rule of this application: given every award won, find the
 * producers who waited the longest between two consecutive wins and the ones who
 * won twice the fastest.
 *
 * <p>Deliberately free of framework, persistence and HTTP concerns, so it can be
 * exercised with nothing but a list of wins.
 */
public class AwardIntervalCalculator {

    public AwardIntervals calculate(Collection<ProducerWin> wins) {
        List<AwardInterval> intervals = consecutiveIntervals(wins);
        if (intervals.isEmpty()) {
            return AwardIntervals.empty();
        }

        IntSummaryStatistics stats = intervals.stream()
                .mapToInt(AwardInterval::interval)
                .summaryStatistics();

        return new AwardIntervals(tiesAt(intervals, stats.getMin()), tiesAt(intervals, stats.getMax()));
    }

    /**
     * Pairs each win with the previous win of the same producer. A producer who
     * won only once contributes no interval and disappears from the result.
     */
    private List<AwardInterval> consecutiveIntervals(Collection<ProducerWin> wins) {
        Map<String, List<Integer>> yearsByProducer = wins.stream()
                .collect(Collectors.groupingBy(ProducerWin::producer,
                        Collectors.mapping(ProducerWin::year, Collectors.toList())));

        List<AwardInterval> intervals = new ArrayList<>();
        yearsByProducer.forEach((producer, years) -> {
            List<Integer> sorted = years.stream().sorted().toList();
            for (int i = 1; i < sorted.size(); i++) {
                int previousWin = sorted.get(i - 1);
                int followingWin = sorted.get(i);
                intervals.add(new AwardInterval(producer, followingWin - previousWin, previousWin, followingWin));
            }
        });
        return intervals;
    }

    /** Every producer sharing the given interval, oldest win first, then alphabetically. */
    private List<AwardInterval> tiesAt(List<AwardInterval> intervals, int interval) {
        return intervals.stream()
                .filter(candidate -> candidate.interval() == interval)
                .sorted(Comparator.comparingInt(AwardInterval::previousWin)
                        .thenComparing(AwardInterval::producer))
                .toList();
    }
}
