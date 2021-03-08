package de.lit.jobscheduler.impl;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import de.lit.jobscheduler.Job;
import de.lit.jobscheduler.JobImplementationProvider;
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
	private final Logger logger = LoggerFactory.getLogger(JobSchedulerTest.class);

	@Autowired
	private JobDefinitionDao jobDao;

	@Autowired
	private JobScheduler jobScheduler;

	private static int parallelCount = 0;
	private static int maxParallelCount = 0;
	private static int job1Count = 0;
	private static int job2Count = 0;

	@Bean
	public Job testjob1() {
		return job -> {
			logger.info(
					String.format("Job %s is running on %s",
							job.getJobDefinition().getName(),
							job.getNodeName()
					));
			parallelCount++;
			maxParallelCount = Math.max(maxParallelCount, parallelCount);
			Thread.sleep(250);
			job1Count++;
			maxParallelCount = Math.max(maxParallelCount, parallelCount);
			parallelCount--;
		};
	}

	@Bean
	public Job testjob2() {
		return job -> {
			logger.info(
					String.format("Job %s is running on %s",
							job.getJobDefinition().getName(),
							job.getNodeName()
					));
			parallelCount++;
			maxParallelCount = Math.max(maxParallelCount, parallelCount);
			Thread.sleep(250);
			job2Count++;
			maxParallelCount = Math.max(maxParallelCount, parallelCount);
			parallelCount--;
		};
	}

	private static LocalDateTime dummyNextRun = LocalDateTime.of(2099, 4, 1, 11, 11);

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
		job1Count = job2Count = 0;
		parallelCount = maxParallelCount = 0;
	}

	@Test
	@DatabaseSetup("testjob1.dataset.xml")
	public void testConcurrentSingleJob() throws Exception {
		int upd = jobDao.runJobNow("testjob1");
		assertEquals("testjob1", 1, upd);

		Thread t1 = new Thread(jobScheduler::run);
		Thread t2 = new Thread(jobScheduler::run);

		t1.start();
		t2.start();

		waitForCondition(10, i ->
				job1Count > 0 && !jobDao.findById("testjob1").orElseThrow(AssertionError::new).isRunning()
		);

		t1.join(1000);
		t2.join(1000);
		Thread.sleep(500);

		assertEquals("job1Count", 1, job1Count);
		JobDefinition job = jobDao.findById("testjob1").orElseThrow(AssertionError::new);
		assertNotNull("testjob1", job);
		assertNotNull("last execution", job.getLastExecution());
		logger.info("Job execution status: " + job.getLastExecution().getStatus());
		assertEquals("execution status", JobExecution.Status.SUCCESS, job.getLastExecution().getStatus());
	}

	@Test
	@DatabaseSetup("testjob12.dataset.xml")
	public void testConcurrentTwoJobs() throws Exception {
		int upd = jobDao.runJobNow("testjob1");
		assertEquals("testjob1", 1, upd);
		upd = jobDao.runJobNow("testjob2");
		assertEquals("testjob2", 1, upd);

		Thread t1 = new Thread(jobScheduler::run);
		Thread t2 = new Thread(jobScheduler::run);

		t1.start();
		t2.start();

		waitForCondition(10, i ->
				job1Count > 0 && job2Count > 0
		);

		t1.join(1000);
		t2.join(1000);
		Thread.sleep(500);

		assertEquals("job1Count", 1, job1Count);
		assertEquals("job2Count", 1, job2Count);
		assertEquals("parallel", 2, maxParallelCount);
		JobDefinition job1 = jobDao.findById("testjob1").orElseThrow(AssertionError::new);
		JobDefinition job2 = jobDao.findById("testjob2").orElseThrow(AssertionError::new);
		assertEquals("job1 status", JobExecution.Status.SUCCESS, job1.getLastExecution().getStatus());
		assertEquals("job2 status", JobExecution.Status.SUCCESS, job2.getLastExecution().getStatus());
	}

	@Test
	@DatabaseSetup("testjob-singleq.dataset.xml")
	public void testConcurrentSingleQueue() throws Exception {
		int upd = jobDao.runJobNow("testjob1");
		assertEquals("testjob1", 1, upd);
		upd = jobDao.runJobNow("testjob2");
		assertEquals("testjob2", 1, upd);

		Thread t1 = new Thread(jobScheduler::run);
		Thread t2 = new Thread(jobScheduler::run);

		t1.start();
		t2.start();

		waitForCondition(10, i ->
				job1Count > 0 || job2Count > 0
		);

		t1.join(1000);
		t2.join(1000);
		Thread.sleep(500);

		assertEquals("only one job", 1, job1Count + job2Count);
		assertEquals("parallel", 1, maxParallelCount);
		JobDefinition job1 = jobDao.findById("testjob1").orElseThrow(AssertionError::new);
		JobDefinition job2 = jobDao.findById("testjob2").orElseThrow(AssertionError::new);
		assertTrue("only one job execution", job1.getLastExecution() == null || job2.getLastExecution() == null);
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
		assertEquals("job1Count", 0, job1Count);
		testjob1 = jobDao.findById("testjob1").orElseThrow(AssertionError::new);
		assertNotNull("testjob1", testjob1);
		assertEquals("nextRun", dummyNextRun, testjob1.getNextRun());
	}

	@Test
	@DatabaseSetup("testjob1.dataset.xml")
	public void testImplProvider() throws Exception {
		JobImplementationProvider old = jobScheduler.getJobImplementationProvider();
		try {
			JobDefinition testjob1 = jobDao.findById("testjob1").orElseThrow(AssertionError::new);
			testjob1.setImplementation("test:customProvider");
			jobDao.save(testjob1);
			jobDao.runJobNow(testjob1.getName());
			jobScheduler.setJobImplementationProvider(jobDefinition -> job -> {
				logger.info("Running custom Job implementation");
				Thread.sleep(250);
				job1Count += 100;
			});

			jobScheduler.run();

			Thread.sleep(500);
			assertEquals("job1Count", 100, job1Count);
		} finally {
			jobScheduler.setJobImplementationProvider(old);
		}
	}

	@Test
	@DatabaseSetup("testjob1.dataset.xml")
	public void testEmptyNextRun() throws Exception {
		JobDefinition testjob1 = jobDao.findById("testjob1").orElseThrow(AssertionError::new);
		testjob1.setNextRun(null);
		testjob1.setLastExecution(null);
		jobDao.save(testjob1);

		jobScheduler.run();

		Thread.sleep(500);
		assertEquals("job1Count", 0, job1Count);
		testjob1 = jobDao.findById("testjob1").orElseThrow(AssertionError::new);
		assertNotNull("testjob1", testjob1);
		assertNull("nextRun", testjob1.getNextRun());
		assertNull("lastExecution", testjob1.getLastExecution());
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
