package de.lit.jobscheduler.dao;

import de.lit.jobscheduler.entity.JobDefinition;
import de.lit.jobscheduler.entity.JobExecution;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static de.lit.jobscheduler.entity.JobExecution.Status.RUNNING;

public interface JobExecutionDao {

	JobExecution create();

	/**
	 * Saves a given entity. Use the returned instance for further operations as the save operation might have changed the
	 * entity instance completely.
	 *
	 * @param entity to save
	 * @return saved entity
	 */
	JobExecution save(JobExecution entity);

	/**
	 * {@code SELECT * FROM JobExecution WHERE id=?1}
	 */
	Optional<JobExecution> findById(Long id);

	List<JobExecution> findAllByJobDefinitionAndStatus(JobDefinition job, JobExecution.Status status);

	List<JobExecution> findAllByJobDefinitionName(String name);

	List<JobExecution> findAllByStatusAndNodeName(JobExecution.Status status, String nodeName);

	List<JobExecution> findAllByStatusAndSignOfLifeTimeBefore(JobExecution.Status status, Date before);

	int updateSignOfLife(Long id, Date timestamp);
}
