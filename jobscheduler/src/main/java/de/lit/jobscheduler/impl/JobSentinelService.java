package de.lit.jobscheduler.impl;

import de.lit.jobscheduler.entity.JobExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
@ConditionalOnProperty("application.jobscheduler.sentinel.enable")
public class JobSentinelService implements InitializingBean {
	private Logger logger = LoggerFactory.getLogger(JobSentinelService.class);

	private ConsistencyCheck consistencyCheck;
	@Value("${application.jobscheduler.sentinel.graceperiod:15}")
	private int gracePeriod = 15;

	@Autowired
	public JobSentinelService(ConsistencyCheck consistencyCheck) {
		this.consistencyCheck = consistencyCheck;
	}

	@Override
	public void afterPropertiesSet() {
		logger.info("Job sentinel activated. Grace period {} minutes.", gracePeriod);
	}

	@Scheduled(
			fixedDelayString = "${application.jobscheduler.sentinel.update.interval:60000}",
			initialDelayString = "${application.jobscheduler.sentinel.update.initial:20000}")
	public void signOfLifeUpdate() {
		consistencyCheck.updateSignOfLife();
	}

	@Scheduled(
			fixedDelayString = "${application.jobscheduler.sentinel.runinterval:70000}",
			initialDelayString = "${application.jobscheduler.sentinel.initialDelay:900000}")
	public void resetDeadJobs() {
		List<JobExecution> executions = consistencyCheck.findDeadExecutions(Duration.ofMinutes(gracePeriod));
		for (JobExecution exec : executions) {
			consistencyCheck.abortRunningJob(exec, "aborted by JobSentinelService");
		}
	}

}
