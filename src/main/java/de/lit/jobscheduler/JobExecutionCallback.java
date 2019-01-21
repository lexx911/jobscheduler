// Copyright (c) 2019 Alexander Lätsch, Lätsch IT Consulting GmbH
// This code is licensed under MIT license (see LICENSE.txt for details)

package de.lit.jobscheduler;

import de.lit.jobscheduler.entity.JobDefinition;
import de.lit.jobscheduler.entity.JobExecution;

interface JobExecutionCallback {

	/**
	 * Called before job actually starts executing.
	 * 
	 * @param job
	 *            JobDefinition
	 * @param jobExecution
	 *            current execution
	 */
	void jobStarted(JobDefinition job, JobExecution jobExecution);

	/**
	 * Called when job has finished execution, either with error or not.
	 *
	 * @param job
	 *            JobDefinition
	 */
	void jobFinished(JobDefinition job);
}
