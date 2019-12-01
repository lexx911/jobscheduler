// Copyright (c) 2019 Alexander Lätsch, Lätsch IT Consulting GmbH
// This code is licensed under MIT license (see LICENSE.txt for details)

package de.lit.jobscheduler.impl;

import de.lit.jobscheduler.CronSchedule;
import de.lit.jobscheduler.Job;
import de.lit.jobscheduler.JobImplementationProvider;
import de.lit.jobscheduler.JobSchedule;
import de.lit.jobscheduler.dao.JobDefinitionDao;
import de.lit.jobscheduler.entity.JobDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.RejectedExecutionException;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Component
public class JobScheduler implements ApplicationContextAware {
	protected final Logger logger = LoggerFactory.getLogger(JobScheduler.class);

	private final JobDefinitionDao jobDao;

	private final JobExecutor jobExecutor;

	private ApplicationContext appContext;

	/**
	 * Optional provider for Job implementation beans/instances.
	 * Default behaviour is {@code appContext.getBean(job.getImplementation(), Job.class)}
	 */
	private JobImplementationProvider jobImplementationProvider;

	@Autowired
	public JobScheduler(JobDefinitionDao jobDao, JobExecutor jobExecutor) {
		this.jobDao = jobDao;
		this.jobExecutor = jobExecutor;
	}

	@Scheduled(fixedDelayString = "${application.jobscheduler.runinterval:20000}", initialDelayString = "${application.jobscheduler.initialDelay:10000}")
	public void run() {
		for (JobDefinition job : jobDao.findAllDue()) {
			if (jobExecutor.remainingCapacity() == 0) {
				logger.warn("jobExecutor has no capacity left. Job {} cannot run", job.getName());
				break;
			}
			try {
				JobInstance instance = createJobInstance(job);
				JobSchedule schedule = instance.getJobSchedule();
				if (schedule.testJobReady(job)) {
					jobExecutor.submitJob(instance);
				} else {
					LocalDateTime nextRun = schedule.evalNextRun(job);
					jobDao.updateForNextRun(job.getName(), nextRun);
				}
			} catch (RejectedExecutionException e) {
				logger.warn("jobExecutor is full. wait for next schedule cycle for Job {}", job.getName());
				break;
			} catch (Exception e) {
				logger.error("Cannot submit job " + job.getName(), e);
			}
		}
	}

	/**
	 * Create {@link JobInstance} from job definition. Retrieves implementation and schedule bean
	 * from application context. If job schedule is empty then the default {@link CronSchedule} is used.
	 *
	 * @param job Definition from Job Table
	 * @return JobInstance
	 * @throws BeansException if implementation or schedule bean cannot be found
	 */
	public JobInstance createJobInstance(JobDefinition job) throws BeansException {
		JobInstance instance = new JobInstance(job);
		String scheduleBean = isNotEmpty(job.getSchedule()) ? job.getSchedule() : "cronSchedule";
		JobSchedule schedule = appContext.getBean(scheduleBean, JobSchedule.class);
		if (jobImplementationProvider != null) {
			instance.setImplementation(jobImplementationProvider.getImplementation(job));
		} else {
			instance.setImplementation(appContext.getBean(job.getImplementation(), Job.class));
		}
		instance.setSchedule(schedule);
		return instance;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.appContext = applicationContext;
	}

	public JobImplementationProvider getJobImplementationProvider() {
		return jobImplementationProvider;
	}

	@Autowired(required = false)
	public void setJobImplementationProvider(JobImplementationProvider jobImplementationProvider) {
		this.jobImplementationProvider = jobImplementationProvider;
	}
}
