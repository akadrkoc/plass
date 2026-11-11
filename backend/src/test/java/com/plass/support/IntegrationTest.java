package com.plass.support;

import com.plass.auth.TokenService;
import com.plass.event.EventService;
import com.plass.event.SeatRepository;
import com.plass.user.UserRepository;
import com.plass.venue.VenueRepository;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base class for tests that need the full application and a real PostgreSQL database.
 * <p>
 * The database runs in Docker via Testcontainers and is shared by all test classes (one Spring
 * context, one container). Without Docker these tests are skipped instead of failing.
 */
@SpringBootTest(properties = {
        "app.seed.enabled=false",
        "app.booking.expiry-check-interval-ms=3600000"
})
@AutoConfigureMockMvc
@Import(IntegrationTest.TestBeans.class)
@Testcontainers(disabledWithoutDocker = true)
public abstract class IntegrationTest {

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        POSTGRES.start(); // no-op if already running
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    protected TestDataFactory data;

    @Autowired
    protected MutableClock clock;

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestBeans {

        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock();
        }

        @Bean
        TestDataFactory testDataFactory(UserRepository userRepository, VenueRepository venueRepository,
                SeatRepository seatRepository, EventService eventService, TokenService tokenService,
                PasswordEncoder passwordEncoder) {
            return new TestDataFactory(userRepository, venueRepository, seatRepository, eventService,
                    tokenService, passwordEncoder);
        }
    }
}
