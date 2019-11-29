package de.lit.jobscheduler;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Annotation to enable Job Scheduler. Will also enable JpaRepositories and Scheduling.
 *
 * @see org.springframework.data.jpa.repository.config.EnableJpaRepositories
 * @see org.springframework.scheduling.annotation.EnableScheduling
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(JobSchedulerAutoConfiguration.class)
public @interface EnableJobScheduler {
}
