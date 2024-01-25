package de.lit.jobscheduler;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJobScheduler
@EntityScan
@EnableJpaRepositories
public class TestApplication {
}
