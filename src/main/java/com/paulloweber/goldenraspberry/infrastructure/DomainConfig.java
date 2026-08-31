package com.paulloweber.goldenraspberry.infrastructure;

import com.paulloweber.goldenraspberry.domain.service.AwardIntervalCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the domain into the container from the outside, so domain classes stay
 * free of Spring annotations.
 */
@Configuration
public class DomainConfig {
    @Bean
    public AwardIntervalCalculator awardIntervalCalculator() {
        return new AwardIntervalCalculator();
    }
}
