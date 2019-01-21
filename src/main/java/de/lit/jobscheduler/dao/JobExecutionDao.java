package de.lit.jobscheduler.dao;

import de.lit.jobscheduler.entity.JobDefinition;
import de.lit.jobscheduler.entity.JobExecution;

import java.util.List;
import java.util.Optional;

public interface JobExecutionDao {

    /**
     * Saves a given entity. Use the returned instance for further operations as the save operation might have changed the
     * entity instance completely.
     *
     * @param entity
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
}
