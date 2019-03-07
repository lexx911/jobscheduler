package de.lit.jobscheduler.dao;

import de.lit.jobscheduler.entity.JobDefinition;
import de.lit.jobscheduler.entity.JobExecution;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface JobDefinitionDao {

	/**
	 * Saves a given entity. Use the returned instance for further operations as the save operation might have changed the
	 * entity instance completely.
	 *
	 * @param entity the entity to save
	 * @return saved entity
	 */
	JobDefinition save(JobDefinition entity);

	/**
	 * {@code SELECT * FROM JobDefinition WHERE name=?1}
	 */
	Optional<JobDefinition> findById(String name);

	/**
	 * <pre>
	 * SELECT * FROM JobDefinition
	 * WHERE running=0 and disabled=0 and suspended=0
	 *  and nextRun <= ?1
	 * ORDER BY nextRun
	 * </pre>
	 * @return All due jobs
	 */
	List<JobDefinition> findAllDue(LocalDateTime when);

	/**
	 * All job due now, equals <code>findAllDue(LocalDateTime.now())</code>
	 * @return All jobs with nextRun before now
	 */
	default List<JobDefinition> findAllDue() {
		return findAllDue(LocalDateTime.now());

	}

	/**
	 * {@code SELECT * FROM JobDefinition WHERE name=?1 FOR UPDATE}
	 */
	JobDefinition lockJob(String name);

	/**
	 * {@code UPDATE JobDefinition SET suspended=0, nextRun=current_timestamp WHERE name=?}
	 */
	int runJobNow(String name);

	/**
	 * {@code UPDATE JobDefinition SET running=?2 where name=?}
	 */
	int updateRunning(String name, boolean running);

	/**
	 * {@code UPDATE JobDefinition SET running=1, lastExecution=?2 where name=?}
	 */
	int updateStartExecution(String name, JobExecution jobExecution);

	/**
	 * {@code UPDATE JobDefinition SET running=0, nextRun=?2 where name=?1}
	 */
	int updateForNextRun(String name, LocalDateTime nextRun);

	/**
	 * {@code UPDATE JobDefinition SET params=?2 where name=?}
	 */
	int updateParams(String name, String params);
}
