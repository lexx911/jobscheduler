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

	@Query("FROM JobDefinition a "
			+ " WHERE a.running=0 and a.disabled=0 and a.suspended=0 "
			+ "  and a.nextRun <= ?1 "
			+ "  and not exists("
			+ "    SELECT 1 FROM JobDefinition x"
			+ "    WHERE x.runQueue = a.runQueue and x.running = 1"
			+ "  ) "
			+ " ORDER BY a.nextRun")
	List<JobDefinition> findAllDue(LocalDateTime when);

	@Query("FROM JobDefinition WHERE name=?1")
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	JobDefinition lockJob(String name);

	@Query("FROM JobDefinition WHERE runQueue=?1")
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	List<JobDefinition> lockRunQueue(String queue);

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
