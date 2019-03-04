package de.lit.jobscheduler;

import de.lit.jobscheduler.dao.JobDefinitionDao;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@EnableJobScheduler
public class SpringConfigurationTest {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	static {
		SpringDbUnitTestCase.initSystemProperties();
	}

	@Autowired
	private JobDefinitionDao jobDao;

	@Autowired
	private JobSchedulerAutoConfiguration config;


	@Test
	public void contextLoads() {
		assertNotNull("jobScheduler", config);
		assertNotNull("jobDao", jobDao);
		logger.info("Spring context successfully loaded");
	}
}
