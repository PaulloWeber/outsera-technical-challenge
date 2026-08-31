package com.paulloweber.goldenraspberry.application.usecase;

import com.paulloweber.goldenraspberry.application.port.in.GetAwardIntervalsUseCase;
import com.paulloweber.goldenraspberry.application.port.out.MovieRepositoryPort;
import com.paulloweber.goldenraspberry.domain.model.AwardIntervals;
import com.paulloweber.goldenraspberry.domain.service.AwardIntervalCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetAwardIntervalsService implements GetAwardIntervalsUseCase {
    private final MovieRepositoryPort movieRepository;
    private final AwardIntervalCalculator calculator;

    public GetAwardIntervalsService(MovieRepositoryPort movieRepository, AwardIntervalCalculator calculator) {
        this.movieRepository = movieRepository;
        this.calculator = calculator;
    }

    @Override
    @Transactional(readOnly = true)
    public AwardIntervals getAwardIntervals() {
        return calculator.calculate(movieRepository.findAllWins());
    }
}
