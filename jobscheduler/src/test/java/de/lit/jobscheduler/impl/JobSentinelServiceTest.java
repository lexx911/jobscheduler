package de.lit.jobscheduler.impl;

import de.lit.jobscheduler.Job;
import de.lit.jobscheduler.SpringTestCase;
import de.lit.jobscheduler.entity.JobExecution;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
@TestPropertySource(properties = "application.jobscheduler.sentinel.enable=true")
public class JobSentinelServiceTest extends SpringTestCase {

	@Autowired
	private JobSentinelService jobSentinelService;

	@Autowired
	private EntityManager em;

	@MockBean
	private JobExecutor jobExecutorMock;

	@MockBean(name = "testjob1")
	private Job testjob1;

	@Test
	@Sql("testexecution1.dataset.sql")
	public void signOfLifeUpdate() {
		JobInstance instance = setupRunningExecution(100);
		JobExecution exec = instance.getJobExecution();

		jobSentinelService.signOfLifeUpdate();

		exec = em.find(JobExecution.class, exec.getId());
		assertNotNull(exec.getSignOfLifeTime());
		assertTrue(exec.getSignOfLifeTime().after(exec.getStartTime()));
	}

	@Test
	@Sql("testexecution2.dataset.sql")
	public void resetDeadJobs() {
		JobInstance instance = setupRunningExecution(100);
		JobExecution exec = instance.getJobExecution();

		jobSentinelService.resetDeadJobs();

		Mockito.verify(jobExecutorMock).prepareForNextRun(any());
		exec = em.find(JobExecution.class, exec.getId());
		assertEquals(JobExecution.Status.ABORTED, exec.getStatus());
	}

	public JobInstance setupRunningExecution(long executionId) {
		JobExecution exec = em.find(JobExecution.class, executionId);
		JobInstance instance = new JobInstance(exec.getJobDefinition());
		instance.setStartedTime(exec.getStartTime().getTime());
		instance.setJobExecution(exec);
		instance.setThread(Thread.currentThread());

		when(jobExecutorMock.listRunningJobs()).thenReturn(Collections.singletonList(instance));
		return instance;
	}

}