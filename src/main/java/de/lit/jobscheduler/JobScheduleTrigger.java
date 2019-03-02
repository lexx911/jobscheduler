package de.lit.jobscheduler;

import de.lit.jobscheduler.entity.JobDefinition;

import java.time.LocalDateTime;

public interface JobScheduleTrigger extends JobTrigger {
	LocalDateTime evalNextRun(JobDefinition job);
}
