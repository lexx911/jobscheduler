package de.lit.jobscheduler.dao;

import ca.krasnay.sqlbuilder.InsertBuilder;
import ca.krasnay.sqlbuilder.UpdateBuilder;
import de.lit.jobscheduler.entity.JobDefinition;
import de.lit.jobscheduler.entity.JobExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class JdbcJobDefinitionDao implements JobDefinitionDao {

	private JdbcTemplate jdbcTemplate;
	private String tablename = "JOB";

	@Autowired
	public JdbcJobDefinitionDao(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public JobDefinition save(JobDefinition entity) {
		Assert.notNull(entity, "entity must not be null");
		Assert.notNull(entity.getName(), "name must not be null");
		int updCount = update(entity);
		if (updCount == 0) {
			insert(entity);
		}
		return entity;
	}

	public int insert(JobDefinition entity) {
		return jdbcTemplate.update(
				new InsertBuilder(tablename)
						.set("NAME", "?")
						.set("CRON_EXPRESSION", "?")
						.set("IMPLEMENTATION", "?")
						.set("SCHEDULE", "?")
						.set("NEXT_RUN", "?")
						.set("PARAMS", "?")
						.set("RUNNING", "?")
						.set("SUSPENDED", "?")
						.set("DISABLED", "?")
						.set("LAST_EXECUTION_ID", "?")
						.set("ERROR_MAIL_ADDRESS", "?")
						.toString(),
				entity.getName(),
				entity.getCronExpression(),
				entity.getImplementation(),
				entity.getSchedule(),
				entity.getNextRun(),
				entity.getParams(),
				entity.isRunning(),
				entity.isSuspended(),
				entity.isDisabled(),
				entity.getLastExecution() != null ? entity.getLastExecution().getId() : null,
				entity.getErrorMailAddress()

		);
	}

	public int update(JobDefinition entity) {
		return jdbcTemplate.update(
				new UpdateBuilder(tablename)
						.set("CRON_EXPRESSION=?")
						.set("IMPLEMENTATION=?")
						.set("SCHEDULE=?")
						.set("NEXT_RUN=?")
						.set("PARAMS=?")
						.set("RUNNING=?")
						.set("SUSPENDED=?")
						.set("DISABLED=?")
						.set("LAST_EXECUTION_ID=?")
						.set("ERROR_MAIL_ADDRESS=?")
						.where("NAME=?")
						.toString(),
				entity.getCronExpression(),
				entity.getImplementation(),
				entity.getSchedule(),
				entity.getNextRun(),
				entity.getParams(),
				entity.isRunning(),
				entity.isSuspended(),
				entity.isDisabled(),
				entity.getLastExecution() != null ? entity.getLastExecution().getId() : null,
				entity.getErrorMailAddress(),
				entity.getName()
		);
	}

	protected JobDefinition rowMapper(ResultSet rs, int rowNum) throws SQLException {
		JobDefinition entity = mapJobDefinition(rs, "");
		if (rs.getObject("LAST_EXECUTION_ID") != null) {
			setLastExecution(entity, rs.getLong("LAST_EXECUTION_ID"));
		}
		return entity;
	}

	protected void setLastExecution(JobDefinition entity, long lastExecutionId) {
		JobExecution lastExecution = new JobExecution();
		lastExecution.setId(lastExecutionId);
		entity.setLastExecution(lastExecution);
		lastExecution.setJobDefinition(entity);
	}

	public JobDefinition mapJobDefinition(ResultSet rs, String columnNamePrefix) throws SQLException {
		JobDefinition entity = new JobDefinition();
		entity.setName(rs.getString(columnNamePrefix + "NAME"));
		entity.setCronExpression(rs.getString(columnNamePrefix + "CRON_EXPRESSION"));
		entity.setImplementation(rs.getString(columnNamePrefix + "IMPLEMENTATION"));
		entity.setSchedule(rs.getString(columnNamePrefix + "SCHEDULE"));
		entity.setNextRun(toLocalDateTime(rs.getTimestamp(columnNamePrefix + "NEXT_RUN")));
		entity.setParams(rs.getString(columnNamePrefix + "PARAMS"));
		entity.setRunning(rs.getBoolean(columnNamePrefix + "RUNNING"));
		entity.setSuspended(rs.getBoolean(columnNamePrefix + "SUSPENDED"));
		entity.setDisabled(rs.getBoolean(columnNamePrefix + "DISABLED"));
		entity.setErrorMailAddress(rs.getString(columnNamePrefix + "ERROR_MAIL_ADDRESS"));
		return entity;
	}

	public String[] getColumnNames() {
		return new String[]{
				"NAME", "CRON_EXPRESSION", "IMPLEMENTATION", "SCHEDULE", "NEXT_RUN", "PARAMS",
				"RUNNING", "SUSPENDED", "DISABLED", "LAST_EXECUTION_ID", "ERROR_MAIL_ADDRESS"
		};
	}

	@Override
	public Optional<JobDefinition> findById(String name) {
		String sql = "SELECT * FROM " + tablename + " WHERE JOB.NAME = ? ";
		return Optional.ofNullable(
				jdbcTemplate.queryForObject(sql, new Object[]{name}, this::rowMapper)
		);
	}

	public List<JobDefinition> findAllDue(LocalDateTime when) {
		String sql = "SELECT * FROM " + tablename
				+ " WHERE JOB.RUNNING=0 and JOB.DISABLED=0 and JOB.SUSPENDED=0 "
				+ "  and JOB.NEXT_RUN <= ? "
				+ " ORDER BY NEXT_RUN";
		return jdbcTemplate.query(sql, new Object[]{when}, this::rowMapper);
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public JobDefinition lockJob(String name) {
		String sql = "SELECT JOB.* FROM " + tablename + " JOB WHERE JOB.NAME = ? FOR UPDATE";
		return jdbcTemplate.queryForObject(sql, new Object[]{name},
				(rs, rowNum) -> mapJobDefinition(rs, ""));
	}

	@Transactional
	public int runJobNow(String name) {
		return jdbcTemplate.update(
				"UPDATE " + tablename + " SET SUSPENDED=0, NEXT_RUN=current_timestamp WHERE name=?",
				name
		);
	}

	@Transactional
	public int updateRunning(String name, boolean running) {
		return jdbcTemplate.update(
				"UPDATE " + tablename + " SET RUNNING=? where NAME=?",
				running, name
		);
	}

	@Transactional
	public int updateStartExecution(String name, JobExecution jobExecution) {
		Assert.notNull(jobExecution, "JobExecution must not be null");
		Assert.notNull(jobExecution.getId(), "JobExecution must be saved first (ID is missing)");
		return jdbcTemplate.update(
				"UPDATE " + tablename + " SET RUNNING=1, LAST_EXECUTION_ID=? where NAME=?",
				new SqlParameterValue(Types.BIGINT, jobExecution.getId()),
				name
		);
	}

	@Transactional
	public int updateForNextRun(String name, LocalDateTime nextRun) {
		return jdbcTemplate.update(
				"UPDATE " + tablename + " SET RUNNING=0, NEXT_RUN=? where NAME=?",
				new SqlParameterValue(Types.TIMESTAMP, nextRun),
				name
		);
	}

	@Transactional
	public int updateParams(String name, String params) {
		return jdbcTemplate.update(
				"UPDATE " + tablename + " SET PARAMS=? where NAME=?",
				params, name
		);
	}

	protected LocalDateTime toLocalDateTime(Timestamp timestamp) {
		return timestamp != null ? timestamp.toLocalDateTime() : null;
	}

	public JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}

	public String getTablename() {
		return tablename;
	}

	/**
	 * Physical table name. Default is "JOB".
	 */
	public void setTablename(String tablename) {
		this.tablename = tablename;
	}
}
