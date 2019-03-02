// Copyright (c) 2019 Alexander Lätsch, Lätsch IT Consulting GmbH
// This code is licensed under MIT license (see LICENSE.txt for details)

package de.lit.jobscheduler.impl;

import java.util.Collection;
import java.util.concurrent.RejectedExecutionException;

import de.lit.jobscheduler.JobTrigger;
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

	/**
	 * Returns the node name that this JobExecutor uses for {@code JobExecution} entries.
	 * @return node name
	 */
	String getNodeName();

	/**
	 * JobExecutor evaluates the name of the current node automatically from
	 * the following environment variables: <pre>
	 *     NODENAME, HOSTNAME, COMPUTERNAME, POD_NAME
	 * </pre>
	 * If none of these variables are set then {@code InetAddress.getLocalHost().getHostName()} is tried.
	 * <p></p>
	 * On a cloud infrastructure (cloudfoundry, docker etc.) this often leads to a random generated hostname
	 * that cannot be used for {@code ConsistenyCheck}. Use {@code setNodeName()} to set a deterministic name.
	 *
	 * @see ConsistencyCheck#singleNodeInstanceCheck()
	 * @param nodeName new node name
	 */
	void setNodeName(String nodeName);

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
}