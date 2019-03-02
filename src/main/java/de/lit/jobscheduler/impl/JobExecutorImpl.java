// Copyright (c) 2019 Alexander Lätsch, Lätsch IT Consulting GmbH
// This code is licensed under MIT license (see LICENSE.txt for details)

package de.lit.jobscheduler.impl;

import de.lit.jobscheduler.Job;
import de.lit.jobscheduler.JobLifecycleCallback;
import de.lit.jobscheduler.JobTrigger;
import de.lit.jobscheduler.dao.JobExecutionDao;
import de.lit.jobscheduler.entity.JobDefinition;
import de.lit.jobscheduler.entity.JobExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.*;

import static de.lit.jobscheduler.entity.JobExecution.Status.*;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

public class JobExecutorImpl extends ThreadPoolExecutor implements JobExecutor, DisposableBean {
	protected final Logger logger = LoggerFactory.getLogger(getClass());

	private String nodeName;

	@Autowired
	private JobExecutionDao jobExecutionDao;

	@Autowired
	@SuppressWarnings("unused")
	// needed as dependency for proper shutdown
	private PlatformTransactionManager txManager;

	@Autowired
	private ApplicationContext appContext;

	private JobLifecycleCallback lifecycleCallback;

	private final Map<Long, JobInstance> runningJobs = Collections.synchronizedMap(new HashMap<>());

	public JobExecutorImpl(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue,
						   ThreadFactory threadFactory, RejectedExecutionHandler handler) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
		nodeName = evaluateHostname().toLowerCase();
	}

	@Override
	protected void beforeExecute(Thread t, Runnable r) {
		if (r instanceof JobInstance) {
			beforeJobExecute(t, (JobInstance) r);
		}
		super.beforeExecute(t, r);
	}

	@Override
	protected void afterExecute(Runnable r, Throwable t) {
		super.afterExecute(r, t);
		if (r instanceof JobInstance) {
			// JobInstance::run never throws an exception but stores it
			afterJobExecute((JobInstance) r);
		}
	}

	protected void beforeJobExecute(Thread t, JobInstance jobInst) {
		synchronized (runningJobs) {
			try {
				logger.info("Starting Job \"{}\"", jobInst.getJob().getName());
				JobExecution jobExec = new JobExecution();
				jobExec.setJobDefinition(jobInst.getJob());
				jobExec.setStartTime(new Date());
				jobExec.setStatus(RUNNING);
				jobExec.setNodeName(nodeName);
				jobExec = jobExecutionDao.save(jobExec);
				jobInst.setJobExecution(jobExec);
				jobInst.setThread(t);
				jobInst.setStartedTime(System.currentTimeMillis());
				runningJobs.put(jobExec.getId(), jobInst);
				jobInst.getCallback().jobStarted(jobInst.getJob(), jobExec);
				if (lifecycleCallback != null) lifecycleCallback.jobStarted(jobInst);
			} catch (Throwable e) {
				logger.error(jobInst.getJob().getName(), e);
				jobInst.setError(e);
				jobInst.getCallback().jobFinished(jobInst.getJob());
				if (lifecycleCallback != null) lifecycleCallback.jobError(jobInst);
			}
		}
	}

	protected void afterJobExecute(JobInstance jobInst) {
		synchronized (runningJobs) {
			JobExecution jobExec = jobExecutionDao.findById(jobInst.getJobExecution().getId())
					.orElseThrow(IllegalArgumentException::new);
			try {
				String jobName = jobExec.getJobDefinition().getName();
				jobExec.setEndTime(new Date());
				long t = System.currentTimeMillis() - jobInst.getStartedTime();
				logger.info("Job \"{}\" completed in {} seconds", jobName, t / 1000.0);
				Throwable error = jobInst.getError();
				if (error != null) {
					logger.error("job " + jobExec.getJobDefinition().getName(), error);
					if (error instanceof InterruptedException) {
						jobExec.setStatus(ABORTED);
					} else {
						jobExec.setStatus(ERROR);
					}

					jobExec.setMessage(formatErrorMessage(jobExec, error));

					if (lifecycleCallback != null) lifecycleCallback.jobError(jobInst);
				} else {
					if (jobExec.getStatus() == null || !jobExec.getStatus().equals(PARTIAL_SUCCESS)) {
						jobExec.setStatus(SUCCESS);
					}
				}
				jobExecutionDao.save(jobExec);
				if (lifecycleCallback != null) lifecycleCallback.jobFinished(jobInst);
				if (jobInst.getTrigger() != null) {
					jobInst.getTrigger().afterJob(jobInst.getJob(), error);
				}
			} catch (Throwable e) {
				logger.error(jobInst.getJob().getName(), e);
				jobExecutionDao.save(jobExec);
			} finally {
				runningJobs.remove(jobExec.getId());
				jobInst.getCallback().jobFinished(jobInst.getJob());
			}
		}
	}

	private String formatErrorMessage(JobExecution jobExec, Throwable error) {
		String message = error.getMessage();
		if (isNotEmpty(jobExec.getMessage())) {
			if (message == null) {
				message = jobExec.getMessage();
			} else {
				message = message + " \n" + jobExec.getMessage();
			}
		}
		if (message != null && message.length() > 4000) {
			message = message.substring(0, 4000);
		}
		return message;
	}

	@Override
	public void execute(JobDefinition job, JobTrigger jobTrigger, JobExecutionCallback callback) throws RejectedExecutionException {
		JobInstance jobInst = new JobInstance(job);
		jobInst.setCallback(callback);
		jobInst.setImplementation(appContext.getBean(job.getImplementation(), Job.class));
		jobInst.setTrigger(jobTrigger);
		if (jobInst.getTrigger() != null) {
			jobInst.getTrigger().beforeJob(jobInst.getJob());
		}
		super.execute(jobInst);
	}

	@Override
	public Collection<JobInstance> listRunningJobs() {
		return runningJobs.values();
	}

	@Override
	public JobInstance getJobInstance(Long jobExecutionId) {
		return runningJobs.get(jobExecutionId);
	}

	@Override
	public String getNodeName() {
		return nodeName;
	}

	@Override
	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	@Override
	public JobInstance findJobInstance(final JobDefinition job) {
		synchronized (runningJobs) {
			Optional<JobInstance> found = runningJobs.values().stream()
					.filter(inst -> inst.getJob().equals(job))
					.findFirst();
			return found.orElse(null);
		}
	}

	@Override
	public int remainingCapacity() {
		return getMaximumPoolSize() - getActiveCount() + getQueue().remainingCapacity();
	}

	@Override
	public void abortJobIfRunning(JobDefinition job) {
		for (JobExecution jobExec : jobExecutionDao.findAllByJobDefinitionAndStatus(job, RUNNING)) {
			JobInstance jobInst = getJobInstance(jobExec.getId());
			if (jobInst != null) {
				jobInst.getThread().interrupt();
			}
			jobExec.setStatus(ABORTED);
			jobExecutionDao.save(jobExec);
		}
	}

	private String evaluateHostname() {
		String[] envVarsToTry = new String[] { "NODENAME", "HOSTNAME", "COMPUTERNAME", "POD_NAME" };
		for (String var : envVarsToTry) {
			if (isNotBlank(System.getenv(var))) {
				return System.getenv(var);
			}
		}
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			logger.warn("Cannot determine local hostname. Please set HOSTNAME environment variable.");
			return "unknown";
		}
	}

	@Override
	public void destroy() throws InterruptedException {
		logger.info("Shutting down JobExecutor");
		shutdown();
		if (!awaitTermination(3, TimeUnit.MINUTES)) {
			logger.info("JobExecutor now interrupting running jobs");
			shutdownNow();
			if (awaitTermination(60, TimeUnit.SECONDS)) {
				logger.warn("JobExecutor shutdown forced threads to interrupt");
			} else {
				logger.error("JobExecutor has hanging theads!");
			}
		} else {
			logger.info("JobExecutor shutdown successful");
		}
		runningJobs.clear();
	}

	public JobLifecycleCallback getLifecycleCallback() {
		return lifecycleCallback;
	}

	public void setLifecycleCallback(JobLifecycleCallback lifecycleCallback) {
		this.lifecycleCallback = lifecycleCallback;
	}
}
