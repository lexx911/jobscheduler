package de.lit.jobscheduler;

import org.springframework.scheduling.support.CronSequenceGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

public class JobCronCalculator {

	public static LocalDateTime eval(String cronExpression) {
		if (isNotEmpty(cronExpression)) {
			String cronExpr = cronExpression;

			// Support for Quartz-like "L"-Notation for Last Day of Month.
			if (cronExpr.contains("L")) {
				cronExpr = cronExpr.replace("L", String.valueOf(LocalDate.now().lengthOfMonth()));
			}
			CronSequenceGenerator cron = new CronSequenceGenerator(cronExpr);
			Date nextDate = cron.next(new Date());
			return nextDate.toInstant()
					.atZone(ZoneId.systemDefault())
					.toLocalDateTime();
		}
		return null;
	}

}
