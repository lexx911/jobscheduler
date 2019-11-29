// Copyright (c) 2019 Alexander Lätsch, Lätsch IT Consulting GmbH
// This code is licensed under MIT license (see LICENSE.txt for details)

package de.lit.jobscheduler.impl;

import de.lit.jobscheduler.Job;
import de.lit.jobscheduler.JobSchedule;
import de.lit.jobscheduler.entity.JobDefinition;
import de.lit.jobscheduler.entity.JobExecution;

public class JobInstance implements Runnable {
	private JobDefinition job;
	private JobExecution jobExecution;
	private Job implementation;
	private JobSchedule jobSchedule;
	private Thread thread;
	private Throwable error;
	private long startedTime;

	public JobInstance(JobDefinition job) {
		this.job = job;
	}

	public void run() {
		try {
			error = null;
			implementation.run(jobExecution);
		} catch (Throwable e) {
			error = e;
		}
	}

	public JobDefinition getJob() {
		return job;
	}

	public void setJob(JobDefinition job) {
		this.job = job;
	}

	public JobExecution getJobExecution() {
		return jobExecution;
	}

	public void setJobExecution(JobExecution jobRun) {
		this.jobExecution = jobRun;
	}

	public Job getImplementation() {
		return implementation;
	}

	public void setImplementation(Job implementation) {
		this.implementation = implementation;
	}

	public JobSchedule getSchedule() {
		return jobSchedule;
	}

	public void setSchedule(JobSchedule jobSchedule) {
		this.jobSchedule = jobSchedule;
	}

	public Thread getThread() {
		return thread;
	}

	public void setThread(Thread thread) {
		this.thread = thread;
	}

	public Throwable getError() {
		return error;
	}

	public void setError(Throwable error) {
		this.error = error;
	}

	public JobSchedule getJobSchedule() {
		return jobSchedule;
	}

	public void setJobSchedule(JobSchedule jobSchedule) {
		this.jobSchedule = jobSchedule;
	}

	public long getStartedTime() {
		return startedTime;
	}

	public void setStartedTime(long startedTime) {
		this.startedTime = startedTime;
	}

}