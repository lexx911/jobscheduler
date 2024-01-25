package de.lit.jobscheduler.impl;

import de.lit.jobscheduler.Job;
import de.lit.jobscheduler.JobImplementationProvider;
import de.lit.jobscheduler.JobSchedule;
import de.lit.jobscheduler.SpringTestCase;
import de.lit.jobscheduler.dao.JobDefinitionDao;
import de.lit.jobscheduler.entity.JobDefinition;
import de.lit.jobscheduler.entity.JobExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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


@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
@ActiveProfiles("JobSchedulerTest")
public class JobSchedulerTest extends SpringTestCase {
    private final Logger logger = LoggerFactory.getLogger(JobSchedulerTest.class);

    @Autowired
    private JobDefinitionDao jobDao;

    @Autowired
    private JobScheduler jobScheduler;

    private static int parallelCount = 0;
    private static int maxParallelCount = 0;
    private static int job1Count = 0;
    private static int job2Count = 0;

    private static LocalDateTime dummyNextRun = LocalDateTime.of(2099, 4, 1, 11, 11);

    @Configuration
    @Profile("JobSchedulerTest")
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
    }

    @BeforeEach
    public void setup() {
        job1Count = job2Count = 0;
        parallelCount = maxParallelCount = 0;
    }

    @Test
    @Sql("testjob1.dataset.sql")
    public void testConcurrentSingleJob() throws Exception {
        int upd = jobDao.runJobNow("TESTJOB1");
        assertEquals(1, upd, "testjob1");

        Thread t1 = new Thread(jobScheduler::run);
        Thread t2 = new Thread(jobScheduler::run);

        t1.start();
        t2.start();

        waitForCondition(10, i ->
                job1Count > 0 && !jobDao.findById("TESTJOB1").orElseThrow(AssertionError::new).isRunning()
        );

        t1.join(1000);
        t2.join(1000);
        Thread.sleep(500);

        assertEquals(1, job1Count, "job1Count");
        JobDefinition job = jobDao.findById("TESTJOB1").orElseThrow(AssertionError::new);
        assertNotNull(job, "testjob1");
        assertNotNull(job.getLastExecution(), "last execution");
        logger.info("Job execution status: " + job.getLastExecution().getStatus());
        assertEquals(JobExecution.Status.SUCCESS, job.getLastExecution().getStatus(), "execution status");
    }

    @Test
    @Sql("testjob12.dataset.sql")
    public void testConcurrentTwoJobs() throws Exception {
        int upd = jobDao.runJobNow("TESTJOB1");
        assertEquals(1, upd, "testjob1");
        upd = jobDao.runJobNow("TESTJOB2");
        assertEquals(1, upd, "testjob2");

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

        assertEquals(1, job1Count, "job1Count");
        assertEquals(1, job2Count, "job2Count");
        assertEquals(2, maxParallelCount, "parallel");
        JobDefinition job1 = jobDao.findById("TESTJOB1").orElseThrow(AssertionError::new);
        JobDefinition job2 = jobDao.findById("TESTJOB2").orElseThrow(AssertionError::new);
        assertEquals(JobExecution.Status.SUCCESS, job1.getLastExecution().getStatus(), "job1 status");
        assertEquals(JobExecution.Status.SUCCESS, job2.getLastExecution().getStatus(), "job2 status");
    }

    @Test
    @Sql("testjob-singleq.dataset.sql")
    public void testConcurrentSingleQueue() throws Exception {
        int upd = jobDao.runJobNow("TESTJOB1");
        assertEquals(1, upd, "testjob1");
        upd = jobDao.runJobNow("TESTJOB2");
        assertEquals(1, upd, "testjob2");

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

        assertEquals(1, job1Count + job2Count, "only one job");
        assertEquals(1, maxParallelCount, "parallel");
        JobDefinition job1 = jobDao.findById("TESTJOB1").orElseThrow(AssertionError::new);
        JobDefinition job2 = jobDao.findById("TESTJOB2").orElseThrow(AssertionError::new);
        assertTrue(job1.getLastExecution() == null || job2.getLastExecution() == null, "only one job execution");
    }

    @Test
    @Sql("testjob1.dataset.sql")
    public void testSchedule() throws Exception {
        JobDefinition testjob1 = jobDao.findById("TESTJOB1").orElseThrow(AssertionError::new);
        testjob1.setSchedule("dummySchedule");
        jobDao.save(testjob1);
        jobDao.runJobNow(testjob1.getName());

        jobScheduler.run();

        Thread.sleep(500);
        assertEquals(0, job1Count, "job1Count");
        testjob1 = jobDao.findById("TESTJOB1").orElseThrow(AssertionError::new);
        assertNotNull(testjob1, "testjob1");
        assertEquals(dummyNextRun, testjob1.getNextRun(), "nextRun");
    }

    @Test
    @Sql("testjob1.dataset.sql")
    public void testImplProvider() throws Exception {
        JobImplementationProvider old = jobScheduler.getJobImplementationProvider();
        try {
            JobDefinition testjob1 = jobDao.findById("TESTJOB1").orElseThrow(AssertionError::new);
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
            assertEquals(100, job1Count,"job1Count");
        } finally {
            jobScheduler.setJobImplementationProvider(old);
        }
    }

    @Test
    @Sql("testjob1.dataset.sql")
    public void testEmptyNextRun() throws Exception {
        JobDefinition testjob1 = jobDao.findById("TESTJOB1").orElseThrow(AssertionError::new);
        testjob1.setNextRun(null);
        testjob1.setLastExecution(null);
        jobDao.save(testjob1);

        jobScheduler.run();

        Thread.sleep(500);
        assertEquals(0, job1Count, "job1Count");
        testjob1 = jobDao.findById("TESTJOB1").orElseThrow(AssertionError::new);
        assertNotNull(testjob1, "testjob1");
        assertNull(testjob1.getNextRun(), "nextRun");
        assertNull(testjob1.getLastExecution(), "lastExecution");
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
