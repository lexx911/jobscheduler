package de.lit.jobscheduler;

import de.lit.jobscheduler.entity.JobDefinition;

@FunctionalInterface
public interface JobImplementationProvider {

	/**
	 * Function that provides a job implementation bean for a given Job Definition
	 * @param jobDefinition Job Definition
	 * @return Job implementation instance, not null
	 */
	Job getImplementation(JobDefinition jobDefinition);
}
