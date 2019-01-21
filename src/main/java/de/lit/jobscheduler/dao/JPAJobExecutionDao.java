package de.lit.jobscheduler.dao;

import de.lit.jobscheduler.entity.JobDefinition;
import de.lit.jobscheduler.entity.JobExecution;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface JPAJobExecutionDao extends CrudRepository<JobExecution, Long>,JobExecutionDao {

	List<JobExecution> findAllByJobDefinitionAndStatus(JobDefinition job, JobExecution.Status status);

	List<JobExecution> findAllByJobDefinitionName(String name);

	List<JobExecution> findAllByStatusAndNodeName(JobExecution.Status status, String nodeName);
}
