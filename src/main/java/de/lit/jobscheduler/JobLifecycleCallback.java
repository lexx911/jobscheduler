// Copyright (c) 2019 Alexander Lätsch, Lätsch IT Consulting GmbH
// This code is licensed under MIT license (see LICENSE.txt for details)

package de.lit.jobscheduler;

public interface JobLifecycleCallback {

	/**
	 * Method invoked prior to executing the given JobInstance.
	 * This method is invoked by the thread that will execute the job,
	 * and may be used to re-initialize ThreadLocals, or to perform logging.
	 * 
	 * @param jobInstance
	 *            JobInstance
	 */
	void jobStarted(JobInstance jobInstance);

	/**
	 * Method invoked when Job has thrown an Exception.
	 * This method is invoked by the thread that executed the task.
	 * Call {@code jobInstance.getError()} to get the Exception that was thrown.
	 *
	 * @param jobInstance
	 *            JobInstance
	 */
	void jobError(JobInstance jobInstance);

	/**
	 * Method invoked upon completion of execution of the given JobInstance.
	 * This method is always invoked by the thread that executed the task - either successfully
	 * or with an exception. {@code jobInstance.getError()} returns a non-null value if there
	 * was an exception.
	 *
	 * @param jobInstance
	 *            JobInstance
	 */
	void jobFinished(JobInstance jobInstance);
}
