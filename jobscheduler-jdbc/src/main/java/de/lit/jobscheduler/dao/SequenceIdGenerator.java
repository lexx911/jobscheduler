package de.lit.jobscheduler.dao;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.function.Supplier;

/**
 * This class may be used as Id-Generator for {@link JdbcJobExecutionDao} if you are
 * using database sequences. Just specify the sequence name and optionally
 * {@code withFromDual()} if your database needs it (like Oracle).
 */
public class SequenceIdGenerator implements Supplier<Long> {
	private JdbcTemplate jdbcTemplate;
	private String sequenceName;
	private String selectSql;

	public SequenceIdGenerator(JdbcTemplate jdbcTemplate, String sequenceName) {
		this.jdbcTemplate = jdbcTemplate;
		this.sequenceName = sequenceName;
		this.selectSql = "SELECT " + sequenceName + ".NEXTVAL";
	}

	/**
	 * Use select {@code SELECT...FROM DUAL} instead of just {@code SELECT...}.
	 * @return this instance for chaining
	 */
	public SequenceIdGenerator withFromDual() {
		this.selectSql = "SELECT " + sequenceName + ".NEXTVAL FROM DUAL";
		return this;
	}

	/**
	 * Specify the select clause if the default {@code SELECT sequence_name.NEXTVAL [FROM DUAL]}
	 * does not fit.
	 * @return this instance for chaining
	 */
	public SequenceIdGenerator withSelect(String sql) {
		this.selectSql = sql;
		return this;
	}

	@Override
	public Long get() {
		return jdbcTemplate.queryForObject(selectSql, Long.TYPE);
	}
}
