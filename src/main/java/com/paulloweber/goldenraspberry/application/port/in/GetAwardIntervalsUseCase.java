package com.paulloweber.goldenraspberry.application.port.in;

import com.paulloweber.goldenraspberry.domain.model.AwardIntervals;

/** Driving port: read the shortest and longest gaps between consecutive awards. */
public interface GetAwardIntervalsUseCase {
    AwardIntervals getAwardIntervals();
}
