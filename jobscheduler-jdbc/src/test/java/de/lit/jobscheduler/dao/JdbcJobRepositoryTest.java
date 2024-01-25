package de.lit.jobscheduler.dao;

import de.lit.jobscheduler.SpringDbUnitTestCase;
import de.lit.jobscheduler.entity.JobDefinition;
import de.lit.jobscheduler.entity.JobExecution;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.Assert.*;

@ContextConfiguration
public class JdbcJobRepositoryTest extends SpringDbUnitTestCase {

	@Autowired
	private JobDefinitionDao jobDao;

	@Autowired
	private JobExecutionDao executionDao;

	public static class JobExecutionIdGenerator implements Supplier<Long> {
		private JdbcTemplate jdbcTemplate;

		@Autowired
		public JobExecutionIdGenerator(JdbcTemplate jdbcTemplate) {
			this.jdbcTemplate = jdbcTemplate;
		}

		@Override
		public Long get() {
			return jdbcTemplate.queryForObject("SELECT job_execution_seq.nextval from dual", Long.TYPE);
		}
	}

	@Test
	@Transactional
	public void testJobDao() throws Exception {
		String testName = "__TEST1";
		JobDefinition testjob = createJobDefinition(testName);
		jobDao.save(testjob);

		JobDefinition job = jobDao.findById(testName).orElseThrow(AssertionError::new);
		assertEquals(testName, job.getName());
		assertThat(job, samePropertyValuesAs(testjob));
		assertNotNull(job.getNextRun());

		List<JobDefinition> list = jobDao.findAllDue();
		assertEquals(1, list.size());
		assertEquals(testjob, list.get(0));

		job = jobDao.lockJob(job.getName());
		job.setRunning(true);
		jobDao.save(job);

		job = jobDao.findById(testName).orElseThrow(AssertionError::new);

		assertEquals(testName, job.getName());
		assertTrue("running", job.isRunning());

		LocalDateTime nextRun = LocalDateTime.now().plusHours(1);
		jobDao.updateForNextRun(testName, nextRun);

		job = jobDao.findById(testName).orElseThrow(AssertionError::new);
		assertEquals(testName, job.getName());
		assertEquals(nextRun, job.getNextRun());
		assertFalse("running", job.isRunning());

		list = jobDao.findAllDue();
		assertEquals(0, list.size());

		jobDao.runJobNow(testName);

		list = jobDao.findAllDue();
		assertEquals(1, list.size());

		jobDao.updateRunning(testName, true);
		jobDao.updateParams(testName, "{\"param\":1}");
		job = jobDao.findById(testName).orElseThrow(AssertionError::new);
		assertTrue("running", job.isRunning());
		assertEquals("{\"param\":1}", job.getParams());
	}

	@Test
	@Transactional
	public void testExecutionDao() throws Exception {
		JobExecution testexec = createJobExecution();
		JobDefinition testJob = jobDao.save(testexec.getJobDefinition());
		JobExecution exec = executionDao.save(testexec);
		assertNotNull(exec.getId());
		Long testId = exec.getId();

		exec = executionDao.findById(testId).orElseThrow(AssertionError::new);
		assertEquals(exec.getId(), testId);
		assertEquals(exec, testexec);
		assertThat(exec, samePropertyValuesAs(testexec));

		List<JobExecution> list = executionDao.findAllByJobDefinitionAndStatus(testJob, testexec.getStatus());
		assertEquals(1, list.size());
		assertEquals(testexec, list.get(0));

		list = executionDao.findAllByStatusAndNodeName(testexec.getStatus(), testexec.getNodeName());
		assertEquals(1, list.size());
		assertEquals(testexec, list.get(0));

		list = executionDao.findAllByJobDefinitionName(testJob.getName());
		assertEquals(1, list.size());
		assertEquals(testexec, list.get(0));

		Timestamp newtime = new Timestamp(System.currentTimeMillis());
		exec.setEndTime(newtime);
		exec.setStatus(JobExecution.Status.SUCCESS);
		exec.setMessage("new test-message");
		executionDao.save(exec);
		exec = executionDao.findById(testId).orElseThrow(AssertionError::new);
		assertEquals(newtime, exec.getEndTime());
		assertEquals(JobExecution.Status.SUCCESS, exec.getStatus());
		assertEquals("new test-message", exec.getMessage());
	}

	@Test
	@Transactional
	public void testReferences() throws Exception {
		String testName = "__TEST3";
		JobDefinition testjob = createJobDefinition(testName);
		jobDao.save(testjob);

		JobExecution testexec = createJobExecution();
		testexec.setJobDefinition(testjob);
		testexec = executionDao.save(testexec);
		jobDao.updateStartExecution(testName, testexec);

		JobDefinition job = jobDao.findById(testName).orElseThrow(AssertionError::new);
		assertNotNull(job.getLastExecution());
		assertEquals(testexec, job.getLastExecution());
		assertNotNull(job.getLastExecution().getJobDefinition());
		assertEquals(job, job.getLastExecution().getJobDefinition());

		JobExecution exec = executionDao.findById(testexec.getId()).orElseThrow(AssertionError::new);
		assertNotNull(exec);
		assertEquals(testjob, exec.getJobDefinition());
		assertEquals(testName, exec.getJobDefinition().getName());
	}

	private JobDefinition createJobDefinition(String testName) {
		JobDefinition job = new JobDefinition();
		job.setName(testName);
		job.setImplementation("testjob1");
		job.setCronExpression("0 0 0 1 1 *");
		job.setSchedule("schedule");
		job.setNextRun(LocalDateTime.now().minusMinutes(1));
		job.setRunning(false);
		job.setDisabled(false);
		job.setSuspended(false);
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
