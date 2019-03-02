package de.lit.jobscheduler;

import de.lit.jobscheduler.entity.JobDefinition;

public class JobTriggerSupport implements JobTrigger {
	@Override
	public boolean isActivated(JobDefinition job) {
		return true;
	}

	@Override
	public void beforeJob(JobDefinition job) {
	}

	@Override
	public void afterJob(JobDefinition job, Throwable error) {
	}
}
