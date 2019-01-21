// Copyright (c) 2019 Alexander Lätsch, Lätsch IT Consulting GmbH
// This code is licensed under MIT license (see LICENSE.txt for details)

package de.lit.jobscheduler;

import de.lit.jobscheduler.entity.JobDefinition;

public interface JobTrigger {

	boolean isActivated(JobDefinition job);

	void beforeJob(JobDefinition job);

	void afterJob(JobDefinition job, Throwable error);

}
