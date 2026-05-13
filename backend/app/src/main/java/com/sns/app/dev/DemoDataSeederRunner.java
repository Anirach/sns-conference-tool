package com.sns.app.dev;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * Runs {@link DemoDataSeeder#seed()} at application boot. Kept separate from the seeder so the
 * seeder itself can be injected into the dev-only reset endpoint without having to bring the
 * ApplicationRunner along for the ride.
 */
@Configuration
class DemoDataSeederRunner {

    @Bean
    @Order(20) // After DevSeedRunner (@Order(10)) so the demo events exist by the time we look them up.
    @ConditionalOnBean(DemoDataSeeder.class)
    ApplicationRunner runDemoSeed(DemoDataSeeder seeder) {
        return args -> seeder.seed();
    }
}
