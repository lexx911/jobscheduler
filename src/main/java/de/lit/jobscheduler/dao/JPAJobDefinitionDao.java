package de.lit.jobscheduler.dao;

import de.lit.jobscheduler.entity.JobDefinition;
import de.lit.jobscheduler.entity.JobExecution;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;

public interface JPAJobDefinitionDao extends CrudRepository<JobDefinition, String>, JobDefinitionDao {

	@Query("FROM JobDefinition "
			+ " WHERE running=0 and disabled=0 and suspended=0 "
			+ "  and (nextRun <= current_timestamp or nextRun is null) "
			+ " ORDER BY nextRun")
	List<JobDefinition> findAllDue();

	@Query("FROM JobDefinition WHERE name=?1")
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	JobDefinition lockJob(String name);

	@Modifying
	@Transactional
	@Query("UPDATE JobDefinition SET suspended=0, nextRun=current_timestamp WHERE name=?1")
	int runJobNow(String name);

	@Modifying
	@Transactional
	@Query("UPDATE JobDefinition SET running=?2 where name=?1")
	int updateRunning(String name, boolean running);

	@Modifying
	@Transactional
	@Query("UPDATE JobDefinition SET running=1, lastExecution=?2 where name=?1")
	int updateStartExecution(String name, JobExecution jobExecution);

	@Modifying
	@Transactional
	@Query("UPDATE JobDefinition SET running=0, nextRun=?2 where name=?1")
	int updateForNextRun(String name, LocalDateTime nextRun);

	@Modifying
	@Transactional
	@Query("UPDATE JobDefinition SET params=?2 where name=?1")
	int updateParams(String name, String params);
}
