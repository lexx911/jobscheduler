package de.lit.jobscheduler.dao;

import ca.krasnay.sqlbuilder.SelectBuilder;
import ca.krasnay.sqlbuilder.UpdateBuilder;
import de.lit.jobscheduler.entity.JobDefinition;
import de.lit.jobscheduler.entity.JobExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Supplier;

public class JdbcJobExecutionDao implements JobExecutionDao {
	private static final String JOBDEF_COLUMN_PREFIX = "JOB_";

	private JdbcTemplate jdbcTemplate;
	private String tablename = "JOB_EXECUTION";
	private Supplier<Long> idGenerator;
	private SimpleJdbcInsert jdbcInsert;
	private JdbcJobDefinitionDao jobDefinitionDao;

	@Autowired
	public JdbcJobExecutionDao(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public JobExecution create() {
		return new JobExecution();
	}

	@Override
	public JobExecution save(JobExecution entity) {
		if (entity.getId() != null) {
			update(entity);
		} else {
			insert(entity);
		}
		return entity;
	}

	public int insert(JobExecution entity) {
		Map<String, Object> params = new HashMap<>();
		params.put("JOB_NAME", entity.getJobDefinition().getName());
		params.put("STATUS", entity.getStatus().name());
		params.put("START_TIME", entity.getStartTime());
		params.put("END_TIME", entity.getEndTime());
		params.put("MESSAGE", entity.getMessage());
		params.put("NODE_NAME", entity.getNodeName());
		params.put("SIGN_OF_LIFE_TIME", entity.getSignOfLifeTime());

		if (idGenerator != null) {
			entity.setId(idGenerator.get());
			params.put("ID", entity.getId());
			getJdbcInsert().execute(params);
		} else {
			Long newId = getJdbcInsert().executeAndReturnKey(params).longValue();
			entity.setId(newId);
		}
		return 1;
	}

	public int update(JobExecution entity) {
		return jdbcTemplate.update(
				new UpdateBuilder(tablename)
						.set("STATUS=?")
						.set("START_TIME=?")
						.set("END_TIME=?")
						.set("MESSAGE=?")
						.set("NODE_NAME=?")
						.set("SIGN_OF_LIFE_TIME=?")
						.where("ID=?")
						.toString(),
				entity.getStatus().name(),
				entity.getStartTime(),
				entity.getEndTime(),
				entity.getMessage(),
				entity.getNodeName(),
				entity.getSignOfLifeTime(),
				entity.getId()
		);
	}

	@Override
	public int updateSignOfLife(Long id, Date timestamp) {
		return jdbcTemplate.update(
				new UpdateBuilder(tablename)
						.set("SIGN_OF_LIFE_TIME=?")
						.where("ID=?")
						.toString(),
				timestamp,
				id
		);
	}

	protected JobExecution rowMapper(ResultSet rs, int rowNum) throws SQLException {
		JobExecution entity = mapJobExecution(rs, "");
		if (rs.getString("JOB_NAME") != null) {
			JobDefinition job = jobDefinitionDao.mapJobDefinition(rs, JOBDEF_COLUMN_PREFIX);
			entity.setJobDefinition(job);
		}
		return entity;
	}

	public JobExecution mapJobExecution(ResultSet rs, String columnNamePrefix) throws SQLException {
		JobExecution entity = create();
		entity.setId(rs.getLong(columnNamePrefix + "ID"));
		entity.setStatus(JobExecution.Status.valueOf(rs.getString(columnNamePrefix + "STATUS")));
		entity.setStartTime(rs.getTimestamp(columnNamePrefix + "START_TIME"));
		entity.setEndTime(rs.getTimestamp(columnNamePrefix + "END_TIME"));
		entity.setMessage(rs.getString(columnNamePrefix + "MESSAGE"));
		entity.setNodeName(rs.getString(columnNamePrefix + "NODE_NAME"));
		entity.setSignOfLifeTime(rs.getTimestamp(columnNamePrefix + "SIGN_OF_LIFE_TIME"));
		return entity;
	}

	private SelectBuilder buildJoinSelect() {
		SelectBuilder sqlBuilder = new SelectBuilder()
				.column("EXECUTION.*")
				.from(tablename + " EXECUTION")
				.leftJoin(jobDefinitionDao.getTablename() + " JOB ON (EXECUTION.JOB_NAME = JOB.NAME) ");

		for (String col : jobDefinitionDao.getColumnNames()) {
			sqlBuilder.column("JOB." + col + " " + JOBDEF_COLUMN_PREFIX + col);
		}

		return sqlBuilder;
	}

	public String[] getColumnNames() {
		return new String[]{"ID", "JOB_NAME", "STATUS", "START_TIME", "END_TIME", "MESSAGE", "NODE_NAME"};
	}

	@Override
	public Optional<JobExecution> findById(Long id) {
		String sql = buildJoinSelect().where("EXECUTION.ID = ?").toString();
		return Optional.ofNullable(
				jdbcTemplate.queryForObject(sql, new Object[]{id}, this::rowMapper)
		);
	}

	@Override
	public List<JobExecution> findAllByJobDefinitionAndStatus(JobDefinition job, JobExecution.Status status) {
		String sql = buildJoinSelect()
				.where("EXECUTION.JOB_NAME = ?")
				.and("EXECUTION.STATUS = ?")
				.toString();
		return jdbcTemplate.query(sql, new Object[]{job.getName(), status.name()}, this::rowMapper);
	}

	@Override
	public List<JobExecution> findAllByJobDefinitionName(String name) {
		String sql = buildJoinSelect()
				.where("EXECUTION.JOB_NAME = ?")
				.toString();
		return jdbcTemplate.query(sql, new Object[]{name}, this::rowMapper);
	}

	@Override
	public List<JobExecution> findAllByStatusAndNodeName(JobExecution.Status status, String nodeName) {
		String sql = buildJoinSelect()
				.where("EXECUTION.STATUS = ?")
				.and("EXECUTION.NODE_NAME = ?")
				.toString();
		return jdbcTemplate.query(sql, new Object[]{status.name(), nodeName}, this::rowMapper);
	}

	@Override
	public List<JobExecution> findAllByStatusAndSignOfLifeTimeBefore(JobExecution.Status status, Date before) {
		String sql = buildJoinSelect()
				.where("EXECUTION.STATUS = ?")
				.and("EXECUTION.SIGN_OF_LIFE_TIME < ?")
				.toString();
		return jdbcTemplate.query(sql, new Object[]{status.name(), before}, this::rowMapper);
	}

	public String getTablename() {
		return tablename;
	}

	public void setTablename(String tablename) {
		this.tablename = tablename;
	}

	public Supplier<Long> getIdGenerator() {
		return idGenerator;
	}

	public void setIdGenerator(Supplier<Long> idGenerator) {
		this.idGenerator = idGenerator;
	}

	public SimpleJdbcInsert getJdbcInsert() {
		if (jdbcInsert == null) {
			jdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
					.withTableName(tablename);
			if (idGenerator == null) {
				jdbcInsert = jdbcInsert.usingGeneratedKeyColumns("ID");
			}
			jdbcInsert.compile();
		}
		return jdbcInsert;
	}

	public void setJdbcInsert(SimpleJdbcInsert jdbcInsert) {
		this.jdbcInsert = jdbcInsert;
	}

	public JdbcJobDefinitionDao getJobDefinitionDao() {
		return jobDefinitionDao;
	}

	@Autowired
	public void setJobDefinitionDao(JdbcJobDefinitionDao jobDefinitionDao) {
		this.jobDefinitionDao = jobDefinitionDao;
	}
}
