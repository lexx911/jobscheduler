package de.lit.jobscheduler;

import de.lit.jobscheduler.entity.JobDefinition;
import org.quartz.CronExpression;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import static org.apache.commons.lang3.StringUtils.isEmpty;

@Component
public class CronSchedule implements JobSchedule {

	@Override
	public boolean testJobReady(JobDefinition job) {
		return true;
	}

	@Override
	public LocalDateTime evalNextRun(JobDefinition job) {
		if (isEmpty(job.getCronExpression())) {
			return null;
		}
		try {
			CronExpression cron = new CronExpression(job.getCronExpression());
			Date nextDate = cron.getNextValidTimeAfter(new Date());
			return nextDate.toInstant()
					.atZone(ZoneId.systemDefault())
					.toLocalDateTime();
		} catch (ParseException e) {
			throw new IllegalArgumentException(job.getCronExpression(), e);
		}
	}


}
