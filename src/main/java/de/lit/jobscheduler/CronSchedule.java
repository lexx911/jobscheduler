package de.lit.jobscheduler;

import de.lit.jobscheduler.entity.JobDefinition;
import org.springframework.scheduling.support.CronSequenceGenerator;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Component
public class CronSchedule implements JobSchedule {

	@Override
	public boolean testJobReady(JobDefinition job) {
		return true;
	}

	@Override
	public LocalDateTime evalNextRun(JobDefinition job) {
		if (isNotEmpty(job.getCronExpression())) {
			CronSequenceGenerator cron = new CronSequenceGenerator(job.getCronExpression());
			Date nextDate = cron.next(new Date());
			return nextDate.toInstant()
					.atZone(ZoneId.systemDefault())
					.toLocalDateTime();
		}
		return null;
	}


}
