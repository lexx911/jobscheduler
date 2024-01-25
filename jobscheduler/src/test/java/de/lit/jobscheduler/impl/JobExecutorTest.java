package de.lit.jobscheduler.impl;

import de.lit.jobscheduler.*;
import de.lit.jobscheduler.dao.JobDefinitionDao;
import de.lit.jobscheduler.entity.JobDefinition;
import de.lit.jobscheduler.entity.JobExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.time.LocalDateTime;
import java.util.function.IntPredicate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
@ActiveProfiles("JobExecutorTest")
public class JobExecutorTest extends SpringTestCase {
	private final Logger logger = LoggerFactory.getLogger(JobExecutorTest.class);

	@Autowired
	private JobDefinitionDao jobDao;

	@Autowired
	private JobUtilityBean jobUtility;

	@Autowired
	private JobExecutor jobExecutor;

	@Mock
	private JobLifecycleCallback lifecycleMock;

	private static int execCount = 0;

	@Configuration
	@Profile("JobExecutorTest")
	static class TestContext {
		private final Logger logger = LoggerFactory.getLogger(JobExecutorTest.class);
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

	@BeforeEach
	public void setup() {
		reset(lifecycleMock);
		((JobExecutorImpl)jobExecutor).setLifecycleCallback(lifecycleMock);
		execCount = 0;
	}

	@Test
	@Sql("testjob1.dataset.sql")
	public void testSuccess() throws Exception {
		JobInstance jobInstance = jobUtility.createJobInstance("testjob1", null);

		jobExecutor.submitJob(jobInstance);

		waitForCondition(100, i ->
				jobIsNotRunning("TESTJOB1")
		);

		assertEquals(1, execCount, "execCount");
		JobDefinition job = jobDao.findById("TESTJOB1").orElseThrow(AssertionError::new);
		assertNotNull(job.getLastExecution(), "last execution");
		logger.info("Job execution status: " + job.getLastExecution().getStatus());
		assertEquals(JobExecution.Status.SUCCESS, job.getLastExecution().getStatus(), "execution status");
		verify(lifecycleMock).jobStarted(any());
		verify(lifecycleMock, never()).jobError(any());
		verify(lifecycleMock).jobFinished(any());
	}

	@Test
	@Sql("testjob2.dataset.sql")
	public void testError() throws Exception {
		JobInstance jobInstance = jobUtility.createJobInstance("testjob2", null);

		jobExecutor.submitJob(jobInstance);

		waitForCondition(100, i ->
				jobIsNotRunning("TESTJOB2")
		);

		JobDefinition job = jobDao.findById("TESTJOB2").orElseThrow(AssertionError::new);
		assertNotNull(job.getLastExecution(), "last execution");
		logger.info("Job execution status: " + job.getLastExecution().getStatus());
		assertEquals(JobExecution.Status.ERROR, job.getLastExecution().getStatus(), "execution status");
		assertEquals("testerror", job.getLastExecution().getMessage(), "execution message");
		System.out.println(mockingDetails(lifecycleMock).printInvocations());
		verify(lifecycleMock).jobStarted(any());
		verify(lifecycleMock).jobError(any());
		verify(lifecycleMock).jobFinished(any());
	}

	@Test
	@Sql("testjob1.dataset.sql")
	public void testSchedule() throws Exception {
		JobInstance jobInstance = jobUtility.createJobInstance("testjob1", null);
		jobInstance.setSchedule(dummySchedule());

		jobExecutor.submitJob(jobInstance);

		waitForCondition(100, i ->
				jobIsNotRunning("TESTJOB1")
		);

		assertEquals(1, execCount, "execCount");
		JobDefinition job = jobDao.findById("TESTJOB1").orElseThrow(AssertionError::new);
		assertEquals(dummyNextRun, job.getNextRun(), "nextRun");
	}

	@Test
	@Sql("testjob1.dataset.sql")
	public void testInvalidCron() throws Exception {
		JobInstance jobInstance = jobUtility.createJobInstance("testjob1", null);
		jobInstance.getJob().setCronExpression("0 0 INVALID");

		jobExecutor.submitJob(jobInstance);

		waitForCondition(100, i ->
				jobIsNotRunning("TESTJOB1")
		);

		assertEquals(1, execCount, "execCount");
		JobDefinition job = jobDao.findById("TESTJOB1").orElseThrow(AssertionError::new);
		assertNull(job.getNextRun(), "nextRun");
	}

	private boolean jobIsNotRunning(String jobname) {
		return !jobDao.findById(jobname).orElseThrow(AssertionError::new).isRunning();
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
