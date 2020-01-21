package de.lit.jobscheduler.impl;

import de.lit.jobscheduler.dao.JobDefinitionDao;
import de.lit.jobscheduler.dao.JobExecutionDao;
import de.lit.jobscheduler.entity.JobDefinition;
import de.lit.jobscheduler.entity.JobExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static de.lit.jobscheduler.entity.JobExecution.Status.ABORTED;
import static de.lit.jobscheduler.entity.JobExecution.Status.RUNNING;

@Component
public class ConsistencyCheck {
	private Logger logger = LoggerFactory.getLogger(ConsistencyCheck.class);

	private final JobExecutionDao jobExecutionDao;
	private final JobDefinitionDao jobDefinitionDao;
	private final JobExecutor jobExecutor;
	private final JobScheduler jobScheduler;

	public ConsistencyCheck(JobExecutor jobExecutor, JobExecutionDao jobExecutionDao, JobDefinitionDao jobDefinitionDao, JobScheduler jobScheduler) {
		this.jobExecutionDao = jobExecutionDao;
		this.jobExecutor = jobExecutor;
		this.jobDefinitionDao = jobDefinitionDao;
		this.jobScheduler = jobScheduler;
	}

	/**
	 * Use this function on application startup to find and remove Jobs
	 * marked as RUNNING on this node that are actually not running on
	 * this {@code JobExecutor} instance right now. These might be
	 * leftovers from a previous crash.
	 * <p></p>
	 * This can only work, if the application is always started on the
	 * same host ({@code nodeName} of {@code JobExecutor})
	 *
	 * @see JobExecutor#getNodeName()
	 */
	public void singleNodeInstanceCheck() {
		List<JobExecution> runningExecs = jobExecutionDao.findAllByStatusAndNodeName(RUNNING, jobExecutor.getNodeName());
		for (JobExecution exec : runningExecs) {
			JobInstance inst = jobExecutor.findJobInstance(exec.getJobDefinition());
			if (inst == null) {
				// check again to be sure. maybe it just finished
				JobDefinition job = jobExecutionDao.findById(exec.getId())
						.orElseThrow(IllegalArgumentException::new)
						.getJobDefinition();
				if (job.isRunning()) {
					logger.warn("Found orphaned job {} execution={}", job.getName(), exec.getId());
					inst = jobScheduler.createJobInstance(job);
					jobExecutor.prepareForNextRun(inst);
				}
			}
		}
	}

	@Transactional
	public void updateSignOfLife() {
		Date now = new Date();
		for (JobInstance inst : jobExecutor.listRunningJobs()) {
			jobExecutionDao.updateSignOfLife(inst.getJobExecution().getId(), now);
		}
	}

	public List<JobExecution> findDeadExecutions(Duration gracePeriod) {
		Date date = new Date(Instant.now().minus(gracePeriod).toEpochMilli());
		return jobExecutionDao.findAllByStatusAndSignOfLifeTimeBefore(RUNNING, date);
	}

	@Transactional
	public void abortRunningJob(JobExecution exec, String message) {
		JobDefinition job = jobDefinitionDao.lockJob(exec.getJobDefinition().getName());
		exec = jobExecutionDao.findById(exec.getId()).orElseThrow(IllegalArgumentException::new);
		if (job.isRunning() && exec.getStatus() == RUNNING) {
			logger.warn("Found orphaned job {} execution={}", job.getName(), exec.getId());
			JobInstance inst = jobScheduler.createJobInstance(job);
			jobExecutor.prepareForNextRun(inst);
		}
		exec.setStatus(ABORTED);
		exec.setEndTime(new Date());
		exec.setMessage(message);
		jobExecutionDao.save(exec);
	}
}
