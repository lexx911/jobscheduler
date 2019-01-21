// Copyright (c) 2019 Alexander Lätsch, Lätsch IT Consulting GmbH
// This code is licensed under MIT license (see LICENSE.txt for details)

package de.lit.jobscheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronSequenceGenerator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.lit.jobscheduler.dao.JobDefinitionDao;
import de.lit.jobscheduler.entity.JobDefinition;
import de.lit.jobscheduler.entity.JobExecution;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
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
					jobDao.updateForNextRun(job.getName(), evalNextRun(job.getCronExpression()));
				}

			} catch (RejectedExecutionException e) {
				// executor is full. wait for next schedule cycle
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

	@PostConstruct
	public void zombieCheck() {
		jobExecutor.zombieCheck(this);
	}

	@Override
	public void jobStarted(JobDefinition job, JobExecution jobExecution) {
		jobDao.updateStartExecution(job.getName(), jobExecution);
	}

	@Override
	public void jobFinished(JobDefinition job) {
		jobDao.updateForNextRun(job.getName(), evalNextRun(job.getCronExpression()));
	}

	public static LocalDateTime evalNextRun(String cronExpression) {
		if (isNotEmpty(cronExpression)) {
			String cronExpr = cronExpression;

			// Support for Quartz-like "L"-Notation for Last Day of Month.
			if (cronExpr.contains("L")) {
				cronExpr = cronExpr.replace("L", Integer.toString(Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH)));
			}
			CronSequenceGenerator cron = new CronSequenceGenerator(cronExpr);
			Date nextDate = cron.next(new Date());
			return nextDate.toInstant()
					.atZone(ZoneId.systemDefault())
					.toLocalDateTime();
		}
		return null;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.appContext = applicationContext;
	}
}
