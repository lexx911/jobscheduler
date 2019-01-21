// Copyright (c) 2019 Alexander Lätsch, Lätsch IT Consulting GmbH
// This code is licensed under MIT license (see LICENSE.txt for details)

package de.lit.jobscheduler;

import java.util.Collection;
import java.util.concurrent.RejectedExecutionException;

import de.lit.jobscheduler.entity.JobDefinition;

public interface JobExecutor {

	/**
	 * Execute job in one of the executors worker threads. The
	 * {@code jobStarted} and {@code jobFinished} callbacks are called from
	 * within the actual worker thread. A JobExecution is created just before
	 * and updated just after the callbacks.
	 * 
	 * @param job
	 *            JobDefinition with implementation to run
	 * @param jobTrigger
	 *            optional JobTrigger providing additional data
	 * @param callback
	 *            optional {@code jobStarted} and {@code jobFinished} callbacks
	 * @throws RejectedExecutionException
	 *             thrown if all workers are occupied and queue is full
	 */
	void execute(JobDefinition job, JobTrigger jobTrigger, JobExecutionCallback callback) throws RejectedExecutionException;

	/**
	 * Provides a list with all currently running job instances.
	 * 
	 * @return Collection of JobInstance
	 */
	Collection<JobInstance> listRunningJobs();

	/**
	 * Return Job Instance from running instances by jobExecutionId. Only for
	 * currently running instances.
	 * 
	 * @param jobExecutionId
	 *            ID of JobExecution
	 * @return JobInstance
	 */
	JobInstance getJobInstance(Long jobExecutionId);

	String getNodeName();

	/**
	 * Find running instance for job definition, if any. Returns {@code null} if
	 * no running instance can be found.
	 * 
	 * @param job
	 *            JobDefinition to search for
	 * @return JobInstance or null
	 */
	JobInstance findJobInstance(JobDefinition job);

	/**
	 * Number of jobs that can still be accepted by {@code execute()}. <br>
	 * <code>remainingCapacity = maximumThreadPoolSize - activeThreads + queueCapacity</code>
	 * 
	 * @return integer
	 */
	int remainingCapacity();

	/**
	 * Try to abort/interrupt all running instances for a job. Sets job
	 * execution status to {@code ABORTED}. Does nothing if there are no running
	 * instances.
	 * 
	 * @param job
	 *            JobDefinition
	 */
	void abortJobIfRunning(JobDefinition job);

	void zombieCheck(JobExecutionCallback callback);
}