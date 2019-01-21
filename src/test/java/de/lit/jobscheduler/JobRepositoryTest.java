package de.lit.jobscheduler;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import de.lit.jobscheduler.dao.JobDefinitionDao;
import de.lit.jobscheduler.entity.JobDefinition;

import java.time.LocalDateTime;

import static org.junit.Assert.*;

public class JobRepositoryTest extends SpringDbUnitTestCase {

	@Autowired
	private JobDefinitionDao jobDao;

	@Test
	@Transactional
	public void test1() throws Exception {
		JobDefinition job = new JobDefinition();
		job.setName("__TEST1");
		job.setImplementation("testjob1");
		job.setNextRun(LocalDateTime.now());
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
}
