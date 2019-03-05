package de.lit.jobscheduler.impl;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import de.lit.jobscheduler.Job;
import de.lit.jobscheduler.JobSchedule;
import de.lit.jobscheduler.SpringDbUnitTestCase;
import de.lit.jobscheduler.dao.JobDefinitionDao;
import de.lit.jobscheduler.entity.JobDefinition;
import de.lit.jobscheduler.entity.JobExecution;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDateTime;
import java.util.function.IntPredicate;

import static org.junit.Assert.*;

@ContextConfiguration
public class JobSchedulerTest extends SpringDbUnitTestCase {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private JobDefinitionDao jobDao;

	@Autowired
	private JobScheduler jobScheduler;

	private static int execCount = 0;

	@Bean
	public Job testjob1() {
		return (JobExecution job, JobSchedule jobSchedule) -> {
			logger.info(
					String.format("Job %s is running on %s",
							job.getJobDefinition().getName(),
							job.getNodeName()
					));
			Thread.sleep(250);
			execCount++;
		};
	}

	private static LocalDateTime dummyNextRun = LocalDateTime.of(2099,4,1,11,11);
	@Bean
	public JobSchedule dummySchedule() {
		return new JobSchedule() {
			@Override
			public boolean testJobReady(JobDefinition job) {
				return false;
			}

			@Override
			public LocalDateTime evalNextRun(JobDefinition job) {
				return dummyNextRun;
			}
		};
	}

	@Before
	public void setup() {
		execCount = 0;
	}

	@Test
	@DatabaseSetup("testjob1.dataset.xml")
	public void testConcurrent() throws Exception {
		int upd = jobDao.runJobNow("testjob1");
		assertEquals("testjob1", 1, upd);

		Thread t1 = new Thread(jobScheduler::run);
		Thread t2 = new Thread(jobScheduler::run);

		t1.start();
		t2.start();

		waitForCondition(10, i ->
				execCount > 0 && !jobDao.findById("testjob1").get().isRunning()
		);

		t1.join(1000);
		t2.join(1000);
		Thread.sleep(500);

		assertEquals("execCount", 1, execCount);
		JobDefinition job = jobDao.findById("testjob1").orElseThrow(AssertionError::new);
		assertNotNull("testjob1", job);
		assertNotNull("last execution", job.getLastExecution());
		logger.info("Job execution status: " + job.getLastExecution().getStatus());
		assertEquals("execution status", JobExecution.Status.SUCCESS, job.getLastExecution().getStatus());
	}

	@Test
	@DatabaseSetup("testjob1.dataset.xml")
	public void testSchedule() throws Exception {
		JobDefinition testjob1 = jobDao.findById("testjob1").orElseThrow(AssertionError::new);
		testjob1.setSchedule("dummySchedule");
		jobDao.save(testjob1);
		jobDao.runJobNow(testjob1.getName());

		jobScheduler.run();

		Thread.sleep(500);
		assertEquals("execCount", 0, execCount);
		testjob1 = jobDao.findById("testjob1").orElseThrow(AssertionError::new);
		assertNotNull("testjob1", testjob1);
		assertEquals("nextRun", dummyNextRun, testjob1.getNextRun());
	}

	private void waitForCondition(int seconds, IntPredicate condition) throws InterruptedException {
		int timeout = seconds * 10;
		do {
			Thread.sleep(100);
			if (condition.test(timeout)) return;
		} while (--timeout > 0);
		fail("Timeout waiting for condition");
	}
}