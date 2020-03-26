package de.lit.jobscheduler.dao;

import de.lit.jobscheduler.entity.JobDefinition;
import de.lit.jobscheduler.entity.JobExecution;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

public interface JPAJobExecutionDao extends CrudRepository<JobExecution, Long>, JobExecutionDao {

	@Override
	default JobExecution create() {
		return new JobExecution();
	}

	List<JobExecution> findAllByJobDefinitionAndStatus(JobDefinition job, JobExecution.Status status);

	List<JobExecution> findAllByJobDefinitionName(String name);

	List<JobExecution> findAllByStatusAndNodeName(JobExecution.Status status, String nodeName);

	@Modifying
	@Transactional
	@Query("update JobExecution set signOfLifeTime=?2 where id=?1")
	int updateSignOfLife(Long id, Date timestamp);

}
