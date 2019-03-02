// Copyright (c) 2019 Alexander Lätsch, Lätsch IT Consulting GmbH
// This code is licensed under MIT license (see LICENSE.txt for details)

package de.lit.jobscheduler.impl;

import de.lit.jobscheduler.Job;
import de.lit.jobscheduler.JobCronCalculator;
import de.lit.jobscheduler.JobTrigger;
import de.lit.jobscheduler.JobScheduleTrigger;
import de.lit.jobscheduler.dao.JobDefinitionDao;
import de.lit.jobscheduler.entity.JobDefinition;
import de.lit.jobscheduler.entity.JobExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.RejectedExecutionException;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Component
public class JobScheduler implements JobExecutionCallback, ApplicationContextAware {
	protected final Logger logger = LoggerFactory.getLogger(getClass());

	private final JobDefinitionDao jobDao;

	private final JobExecutor jobExecutor;

	private ApplicationContext appContext;

	@Autowired
	public JobScheduler(JobDefinitionDao jobDao, JobExecutor jobExecutor) {
		this.jobDao = jobDao;
		this.jobExecutor = jobExecutor;
	}

	@Scheduled(fixedDelayString = "${application.jobscheduler.runinterval:20000}", initialDelayString = "${application.jobscheduler.initialDelay:10000}")
	public void run() {
		for (JobDefinition job : jobDao.findAllDue()) {
			if (jobExecutor.remainingCapacity() == 0) {
				break;
			}
			try {
				JobTrigger jobTrigger = isNotEmpty(job.getTrigger())
						? appContext.getBean(job.getTrigger(), JobTrigger.class)
						: null;
				if (testJobDue(job, jobTrigger)) {
					JobScheduler scheduler = appContext.getBean(JobScheduler.class);
					scheduler.submitJob(job, jobTrigger);
				} else if (!job.isRunning() && !job.isSuspended() && !job.isDisabled()) {
					updateNextRun(job);
				}

			} catch (RejectedExecutionException e) {
				logger.debug("executor is full. wait for next schedule cycle.");
				break;
			} catch (Exception e) {
				logger.error("Cannot submit job " + job.getName(), e);
				jobFinished(job);
			}
		}
	}

	@Transactional
	public void submitJob(JobDefinition job, JobTrigger jobTrigger) throws RejectedExecutionException {
		job = jobDao.lockJob(job.getName());
		if (!testJobDue(job, jobTrigger)) {
			logger.debug("Job {} not executed by this thread", job.getName());
			return;
		}
		jobExecutor.execute(job, jobTrigger, this);
		jobDao.updateRunning(job.getName(), true);
	}

	private boolean testJobDue(JobDefinition job, JobTrigger jobTrigger) {
		return !job.isRunning() && !job.isSuspended() && !job.isDisabled() &&
				(jobTrigger == null || jobTrigger.isActivated(job));
	}

	@Override
	public void jobStarted(JobDefinition job, JobExecution jobExecution) {
		jobDao.updateStartExecution(job.getName(), jobExecution);
	}

	@Override
	public void jobFinished(JobDefinition job) {
		updateNextRun(job);
	}

	private void updateNextRun(JobDefinition job) {
		LocalDateTime nextRun;
		try {
			JobTrigger trigger = appContext.getBean(job.getTrigger(), JobTrigger.class);
			if (trigger instanceof JobScheduleTrigger) {
				nextRun = ((JobScheduleTrigger) trigger).evalNextRun(job);
			} else {
				nextRun = JobCronCalculator.eval(job.getCronExpression());
			}
		} catch (Throwable t) {
			nextRun = JobCronCalculator.eval(job.getCronExpression());
		}
		jobDao.updateForNextRun(job.getName(), nextRun);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.appContext = applicationContext;
	}
}
