package de.lit.jobscheduler;

import de.lit.jobscheduler.dao.JobDefinitionDao;
import de.lit.jobscheduler.dao.JobExecutionDao;
import de.lit.jobscheduler.entity.JobDefinition;
import de.lit.jobscheduler.entity.JobExecution;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@ComponentScan
@EnableJpaRepositories(basePackageClasses = {JobDefinitionDao.class, JobExecutionDao.class})
@EntityScan(basePackageClasses = {JobDefinition.class, JobExecution.class})
@EnableScheduling
public class JobSchedulerAutoConfiguration {
}
