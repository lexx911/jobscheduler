package de.lit.jobscheduler;

import de.lit.jobscheduler.entity.JobDefinition;
import de.lit.jobscheduler.entity.JobExecution;
import de.lit.jobscheduler.impl.JobInstance;
import de.lit.jobscheduler.impl.JobScheduler;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JobUtilityBean {

	private JobScheduler jobScheduler;

	private JobLifecycleCallback lifecycleCallback;

	@Autowired
	public JobUtilityBean(JobScheduler jobScheduler) {
		this.jobScheduler = jobScheduler;
	}

	public JobInstance executeJob(String implementation, String params) throws BeansException {
		return executeJob(createJobInstance(implementation, params));
	}

	public JobInstance createJobInstance(String implementation, String params) throws BeansException {
		JobDefinition job = new JobDefinition();
		job.setName(implementation.toUpperCase());
		job.setImplementation(implementation);
		job.setParams(params);
		return jobScheduler.createJobInstance(job);
	}

	public JobInstance executeJob(JobInstance instance) {

		JobExecution exec = new JobExecution();
		exec.setId(0L);
		exec.setJobDefinition(instance.getJob());
		exec.setStartTime(new Date(System.currentTimeMillis()));
		exec.setStatus(JobExecution.Status.RUNNING);
		instance.setJobExecution(exec);
		instance.setThread(Thread.currentThread());
		instance.setStartedTime(exec.getStartTime().getTime());

		if (lifecycleCallback != null) {
			lifecycleCallback.jobStarted(instance);
		}

		instance.run();

		exec.setEndTime(new Date(System.currentTimeMillis()));
		if (instance.getError() != null) {
			LoggerFactory.getLogger(instance.getImplementation().getClass()).error("Exception in Job " + instance.getJob().getName(), instance.getError());
			if (lifecycleCallback != null) {
				lifecycleCallback.jobError(instance);
			}
		}

		if (lifecycleCallback != null) {
			lifecycleCallback.jobFinished(instance);
		}
		return instance;
	}

	@Autowired(required = false)
	public void setLifecycleCallback(JobLifecycleCallback lifecycleCallback) {
		this.lifecycleCallback = lifecycleCallback;
	}
}
