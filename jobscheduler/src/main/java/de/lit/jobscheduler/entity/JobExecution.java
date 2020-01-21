package de.lit.jobscheduler.entity;

import javax.persistence.*;
import java.util.Date;
import java.util.Objects;

@Entity
public class JobExecution {

	public enum Status {
		RUNNING, SUCCESS, ERROR, ABORTED, PARTIAL_SUCCESS
	}

	@Id
	@GeneratedValue(generator = "job_execution_seq")
	private Long id;

	@ManyToOne
	@JoinColumn(name = "job_name", foreignKey = @ForeignKey(name = "fk_job_execution_job"))
	private JobDefinition jobDefinition;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private Status status;

	private String nodeName;
	@Column(length = 4000)
	private String message;
	@Temporal(TemporalType.TIMESTAMP)
	@Column(columnDefinition = "TIMESTAMP(6)")
	private Date startTime;
	@Temporal(TemporalType.TIMESTAMP)
	@Column(columnDefinition = "TIMESTAMP(6)")
	private Date endTime;
	@Temporal(TemporalType.TIMESTAMP)
	@Column(columnDefinition = "TIMESTAMP(6)")
	private Date signOfLifeTime;

	@Transient
	private boolean interrupted = false;

	@Transient
	public boolean isInterrupted() {
		if (Thread.currentThread().isInterrupted()) {
			interrupted = true;
		}
		return interrupted;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof JobExecution)) return false;
		if (id == null) return false;
		JobExecution that = (JobExecution) o;
		return id.equals(that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public JobDefinition getJobDefinition() {
		return jobDefinition;
	}

	public void setJobDefinition(JobDefinition jobDefinition) {
		this.jobDefinition = jobDefinition;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	public Date getSignOfLifeTime() {
		return signOfLifeTime;
	}

	public void setSignOfLifeTime(Date signOfLifeTime) {
		this.signOfLifeTime = signOfLifeTime;
	}
}
