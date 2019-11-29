// Copyright (c) 2019 Alexander Lätsch, Lätsch IT Consulting GmbH
// This code is licensed under MIT license (see LICENSE.txt for details)

package de.lit.jobscheduler;

import de.lit.jobscheduler.entity.JobDefinition;

import java.time.LocalDateTime;

public interface JobSchedule {

	boolean testJobReady(JobDefinition job);

	LocalDateTime evalNextRun(JobDefinition job);

}
