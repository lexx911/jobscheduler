package de.lit.jobscheduler.impl;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import de.lit.jobscheduler.SpringDbUnitTestCase;
import de.lit.jobscheduler.entity.JobExecution;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import javax.persistence.EntityManager;
import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ContextConfiguration
public class JobSentinelServiceTest extends SpringDbUnitTestCase {

	@Autowired
	private JobSentinelService jobSentinelService;

	@Autowired
	private EntityManager em;

	@Autowired
	private JobExecutor jobExecutorMock;

	@Test
	@DatabaseSetup("testexecution1.dataset.xml")
	public void signOfLifeUpdate() {
		JobInstance instance = setupRunningExecution(100);
		JobExecution exec = instance.getJobExecution();

		jobSentinelService.signOfLifeUpdate();

		exec = em.find(JobExecution.class, exec.getId());
		assertNotNull(exec.getSignOfLifeTime());
		assertTrue(exec.getSignOfLifeTime().after(exec.getStartTime()));
	}

	@Test
	@DatabaseSetup("testexecution2.dataset.xml")
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