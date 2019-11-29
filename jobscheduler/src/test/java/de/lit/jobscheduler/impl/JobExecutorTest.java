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
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDateTime;
import java.util.function.IntPredicate;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@ContextConfiguration
public class JobExecutorTest extends SpringDbUnitTestCase {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private JobDefinitionDao jobDao;

	@Autowired
	private JobUtilityBean jobUtility;

	@Autowired
	private JobExecutor jobExecutor;

	@Autowired
	private JobLifecycleCallback lifecycleMock;

	private static int execCount = 0;

	@Bean
	public Job testjob1() {
		return job -> {
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
		return job -> {
			logger.info(
					String.format("Job %s is running on %s throwing error",
							job.getJobDefinition().getName(),
							job.getNodeName()
					));
			throw new RuntimeException("testerror");
		};
	}

	private static LocalDateTime dummyNextRun = LocalDateTime.of(2088,4,1,11,11);
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
		reset(lifecycleMock);
		((JobExecutorImpl)jobExecutor).setLifecycleCallback(lifecycleMock);
		execCount = 0;
	}

	@Test
	@DatabaseSetup("testjob1.dataset.xml")
	public void testSuccess() throws Exception {
		JobInstance jobInstance = jobUtility.createJobInstance("testjob1", null);

		jobExecutor.submitJob(jobInstance);

		waitForCondition(100, i ->
				jobIsNotRunning("testjob1")
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
	@DatabaseSetup("testjob2.dataset.xml")
	public void testError() throws Exception {
		JobInstance jobInstance = jobUtility.createJobInstance("testjob2", null);

		jobExecutor.submitJob(jobInstance);

		waitForCondition(100, i ->
				jobIsNotRunning("testjob2")
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
	@DatabaseSetup("testjob1.dataset.xml")
	public void testSchedule() throws Exception {
		JobInstance jobInstance = jobUtility.createJobInstance("testjob1", null);
		jobInstance.setSchedule(dummySchedule());

		jobExecutor.submitJob(jobInstance);

		waitForCondition(100, i ->
				jobIsNotRunning("testjob1")
		);

		assertEquals("execCount", 1, execCount);
		JobDefinition job = jobDao.findById("testjob1").orElseThrow(AssertionError::new);
		assertEquals("nextRun", dummyNextRun, job.getNextRun());
	}

	@Test
	@DatabaseSetup("testjob1.dataset.xml")
	public void testInvalidCron() throws Exception {
		JobInstance jobInstance = jobUtility.createJobInstance("testjob1", null);
		jobInstance.getJob().setCronExpression("0 0 INVALID");

		jobExecutor.submitJob(jobInstance);

		waitForCondition(100, i ->
				jobIsNotRunning("testjob1")
		);

		assertEquals("execCount", 1, execCount);
		JobDefinition job = jobDao.findById("testjob1").orElseThrow(AssertionError::new);
		assertNull("nextRun", job.getNextRun());
	}

	private boolean jobIsNotRunning(String testjob1) {
		return !jobDao.findById(testjob1).orElseThrow(AssertionError::new).isRunning();
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
