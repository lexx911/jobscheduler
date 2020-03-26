package de.lit.jobscheduler;

import de.lit.jobscheduler.dao.JobDefinitionDao;
import de.lit.jobscheduler.dao.JobExecutionDao;
import de.lit.jobscheduler.entity.JobDefinition;
import de.lit.jobscheduler.entity.JobExecution;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.junit.Assert.*;

@ContextConfiguration
public class JobRepositoryTest extends SpringDbUnitTestCase {

	@Autowired
	private JobDefinitionDao jobDao;

	@Autowired
	private JobExecutionDao executionDao;

	@Autowired
	protected EntityManager em;

	@Test
	@Transactional
	public void test1() throws Exception {
		JobDefinition job = createJobDefinition("__TEST1");
		job = jobDao.save(job);
		em.flush();

		assertFalse("running", job.isRunning());

		job = jobDao.lockJob(job.getName());
		job.setRunning(true);
		job = jobDao.save(job);
		em.flush();

		assertEquals("name", "__TEST1", job.getName());
		assertTrue("running", job.isRunning());
	}

	@Test
	@Transactional
	public void testReferences() throws Exception {
		String testName = "__TEST3";
		JobDefinition testjob = createJobDefinition(testName);
		testjob = jobDao.save(testjob);

		JobExecution testexec = createJobExecution();
		testexec.setJobDefinition(testjob);
		JobExecution exec = executionDao.save(testexec);
		jobDao.updateStartExecution(testName, exec);
		em.flush();
		em.clear();

		JobDefinition job = jobDao.findById(testName).orElseThrow(AssertionError::new);
		assertNotNull(job.getLastExecution());
		assertEquals(exec, job.getLastExecution());
		assertNotNull(job.getLastExecution().getJobDefinition());
		assertEquals(job, job.getLastExecution().getJobDefinition());
	}

	private JobDefinition createJobDefinition(String testName) {
		JobDefinition job = new JobDefinition();
		job.setName(testName);
		job.setImplementation("testjob1");
		job.setCronExpression("0 0 0 1 1 *");
		job.setSchedule("schedule");
		job.setNextRun(LocalDateTime.now());
		return job;
	}

	private JobExecution createJobExecution() {
		JobExecution exec = executionDao.create();
		exec.setStatus(JobExecution.Status.RUNNING);
		exec.setJobDefinition(createJobDefinition("__TEST2"));
		exec.setStartTime(new Timestamp(System.currentTimeMillis() - 1234));
		exec.setEndTime(new Timestamp(System.currentTimeMillis()));
		exec.setMessage("message text");
		exec.setNodeName("node1name");
		return exec;
	}
}
