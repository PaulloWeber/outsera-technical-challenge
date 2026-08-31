package com.paulloweber.goldenraspberry.adapter.in.web;

import com.paulloweber.goldenraspberry.adapter.in.web.dto.AwardIntervalsResponse;
import com.paulloweber.goldenraspberry.application.port.in.GetAwardIntervalsUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProducerController {
    private final GetAwardIntervalsUseCase getAwardIntervals;

    public ProducerController(GetAwardIntervalsUseCase getAwardIntervals) {
        this.getAwardIntervals = getAwardIntervals;
    }

    @GetMapping({"/api/public/producers/win-intervals", "/api/producers/win-intervals"})
    public AwardIntervalsResponse winIntervals() {
        return AwardIntervalsResponse.from(getAwardIntervals.getAwardIntervals());
    }
}
