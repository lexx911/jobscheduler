package de.lit.jobscheduler;

import de.lit.jobscheduler.dao.JobDefinitionDao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestApplication.class)
@EnableJobScheduler
public class SpringConfigurationTest {
	private final Logger logger = LoggerFactory.getLogger(SpringConfigurationTest.class);

	@Autowired
	private JobDefinitionDao jobDao;

	@Autowired
	private JobSchedulerAutoConfiguration config;


	@Test
	public void contextLoads() {
		assertNotNull(config, "jobScheduler");
		assertNotNull(jobDao, "jobDao");
		logger.info("Spring context successfully loaded");
	}
}
