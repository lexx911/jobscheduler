package de.lit.jobscheduler.dao;

import org.springframework.data.repository.CrudRepository;

import de.lit.jobscheduler.entity.JobDefinition;
import de.lit.jobscheduler.entity.JobExecution;

import java.util.List;

public interface JobExecutionDao extends CrudRepository<JobExecution, Long> {

	List<JobExecution> findAllByJobDefinitionAndStatus(JobDefinition job, JobExecution.Status status);

	List<JobExecution> findAllByJobDefinitionName(String name);

	List<JobExecution> findAllByStatusAndNodeName(JobExecution.Status status, String nodeName);
}
