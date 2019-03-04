package de.lit.jobscheduler.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "job")
public class JobDefinition {

    @Id
    @Column(length = 50)
    private String name;

    private LocalDateTime nextRun;

    /**
     * Cron expression is a list of six single space-separated fields representing:<br>
     * <pre>
     *     second minute hour day month weekday
     * </pre>
     * <p>
     * Month and weekday names can be given as the first three letters of the English names.
     * <p>
     * Example patterns:
     * <pre>
     * "0 0 * * * *" = the top of every hour of every day.
     * "*&#47;10 * * * * *" = every ten seconds.
     * "0 0 8-10 * * *" = 8, 9 and 10 o'clock of every day.
     * "0 0/30 8-10 * * *" = 8:00, 8:30, 9:00, 9:30 and 10 o'clock every day.
     * "0 0 9-17 * * MON-FRI" = on the hour nine-to-five weekdays
     * "0 0 0 25 12 ?" = every Christmas Day at midnight
     * </pre>
     */
    private String cronExpression;

    private String schedule;

    private String implementation;

    private String params;

    private boolean running;

    private boolean suspended;

    private boolean disabled;

    private String errorMailAddress;

    @ManyToOne
    @JoinColumn(foreignKey = @ForeignKey(name = "fk_job_last_execution"))
    private JobExecution lastExecution;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getNextRun() {
        return nextRun;
    }

    public void setNextRun(LocalDateTime nextRun) {
        this.nextRun = nextRun;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public String getSchedule() {
        return schedule;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public String getImplementation() {
        return implementation;
    }

    public void setImplementation(String implementation) {
        this.implementation = implementation;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public boolean isSuspended() {
        return suspended;
    }

    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public JobExecution getLastExecution() {
        return lastExecution;
    }

    public void setLastExecution(JobExecution lastExecution) {
        this.lastExecution = lastExecution;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public String getErrorMailAddress() {
        return errorMailAddress;
    }

    public void setErrorMailAddress(String errorMailAddress) {
        this.errorMailAddress = errorMailAddress;
    }
}
