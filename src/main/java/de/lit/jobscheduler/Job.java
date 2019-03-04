// Copyright (c) 2019 Alexander Lätsch, Lätsch IT Consulting GmbH
// This code is licensed under MIT license (see LICENSE.txt for details)

package de.lit.jobscheduler;

import de.lit.jobscheduler.entity.JobExecution;

public interface Job {
	void run(JobExecution job, JobSchedule jobSchedule) throws Exception;
}
