package de.lit.jobscheduler;

import de.lit.jobscheduler.dao.JobExecutionDao;
import de.lit.jobscheduler.entity.JobDefinition;
import de.lit.jobscheduler.entity.JobExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

import static de.lit.jobscheduler.entity.JobExecution.Status.RUNNING;

@Component
public class ConsistencyCheck {
	private Logger logger = LoggerFactory.getLogger(getClass());

	private JobExecutionDao jobExecutionDao;
	private JobScheduler jobScheduler;
	private JobExecutor jobExecutor;

	public ConsistencyCheck(JobScheduler jobScheduler, JobExecutor jobExecutor, JobExecutionDao jobExecutionDao) {
		this.jobExecutionDao = jobExecutionDao;
		this.jobScheduler = jobScheduler;
		this.jobExecutor = jobExecutor;
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
	 *
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
					jobScheduler.jobFinished(job);
				}
			}
		}
	}
}
