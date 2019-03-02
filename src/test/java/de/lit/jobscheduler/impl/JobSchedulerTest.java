package de.lit.jobscheduler.impl;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import de.lit.jobscheduler.*;
import de.lit.jobscheduler.dao.JobDefinitionDao;
import de.lit.jobscheduler.entity.JobDefinition;
import de.lit.jobscheduler.entity.JobExecution;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import java.util.function.IntPredicate;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ContextConfiguration
public class JobSchedulerTest extends SpringDbUnitTestCase {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private JobDefinitionDao jobDao;

	@Autowired
	private JobScheduler jobScheduler;

	@Autowired
	public JobLifecycleCallback lifecycleMock;

	private static int execCount = 0;

	@Bean
	public Job testjob1() {
		return (JobExecution job, JobTrigger jobTrigger) -> {
			logger.info(
					String.format("Job %s is running on %s",
							job.getJobDefinition().getName(),
							job.getNodeName()
					));
			Thread.sleep(250);
			execCount++;
		};
	}

	@Bean
	public Job testjob2() {
		return (JobExecution job, JobTrigger jobTrigger) -> {
			logger.info(
					String.format("Job %s is running on %s throwing error",
							job.getJobDefinition().getName(),
							job.getNodeName()
					));
			throw new RuntimeException("testerror");
		};
	}

	@Before
	public void setup() {
		reset(lifecycleMock);
		execCount = 0;
	}

	@Test
	@DatabaseSetup("JobSchedulerTest.test1.xml")
	public void testSuccess() throws Exception {
		int upd = jobDao.runJobNow("testjob1");
		assertEquals("testjob1", 1, upd);

		jobScheduler.run();

		waitForCondition(100, i ->
				!jobDao.findById("testjob1").get().isRunning()
		);

		assertEquals("execCount", 1, execCount);
		JobDefinition job = jobDao.findById("testjob1").orElseThrow(AssertionError::new);
		assertNotNull("last execution", job.getLastExecution());
		logger.info("Job execution status: " + job.getLastExecution().getStatus());
		assertEquals("execution status", JobExecution.Status.SUCCESS, job.getLastExecution().getStatus());
		verify(lifecycleMock).jobStarted(any());
		verify(lifecycleMock, never()).jobError(any());
		verify(lifecycleMock).jobFinished(any());
	}

	@Test
	@DatabaseSetup("JobSchedulerTest.test2.xml")
	public void testError() throws Exception {
		int upd = jobDao.runJobNow("testjob2");
		assertEquals("testjob2", 1, upd);

		jobScheduler.run();

		waitForCondition(100, i ->
				!jobDao.findById("testjob2").get().isRunning()
		);

		JobDefinition job = jobDao.findById("testjob2").orElseThrow(AssertionError::new);
		assertNotNull("last execution", job.getLastExecution());
		logger.info("Job execution status: " + job.getLastExecution().getStatus());
		assertEquals("execution status", JobExecution.Status.ERROR, job.getLastExecution().getStatus());
		assertEquals("execution message", "testerror", job.getLastExecution().getMessage());
		System.out.println(mockingDetails(lifecycleMock).printInvocations());
		verify(lifecycleMock).jobStarted(any());
		verify(lifecycleMock).jobError(any());
		verify(lifecycleMock).jobFinished(any());
	}

	@Test
	@DatabaseSetup("JobSchedulerTest.test1.xml")
	public void testConcurrent() throws Exception {
		execCount = 0;
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

	private void waitForCondition(int seconds, IntPredicate condition) throws InterruptedException {
		int timeout = seconds * 10;
		do {
			Thread.sleep(100);
			if (condition.test(timeout)) return;
		} while (--timeout > 0);
		fail("Timeout waiting for condition");
	}
}
