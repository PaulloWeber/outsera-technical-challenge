package com.paulloweber.goldenraspberry.adapter.in.web.dto;

import com.paulloweber.goldenraspberry.domain.model.AwardInterval;

public record ProducerAwardInterval(String producer, int interval, int previousWin, int followingWin) {
    public static ProducerAwardInterval from(AwardInterval interval) {
        return new ProducerAwardInterval(
                interval.producer(), interval.interval(), interval.previousWin(), interval.followingWin());
    }
}
