package com.paulloweber.goldenraspberry.adapter.in.web.dto;

import com.paulloweber.goldenraspberry.domain.model.AwardIntervals;

import java.util.List;

public record AwardIntervalsResponse(List<ProducerAwardInterval> min, List<ProducerAwardInterval> max) {
    public static AwardIntervalsResponse from(AwardIntervals intervals) {
        return new AwardIntervalsResponse(map(intervals.min()), map(intervals.max()));
    }

    private static List<ProducerAwardInterval> map(List<com.paulloweber.goldenraspberry.domain.model.AwardInterval> source) {
        return source.stream().map(ProducerAwardInterval::from).toList();
    }
}
