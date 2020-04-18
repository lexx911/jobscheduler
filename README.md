# Database Job Scheduler ![version badge](https://img.shields.io/maven-central/v/de.laetsch-it.jobscheduler/jobscheduler?label=latest%20version)

## Overview
This library provides a job scheduling mechanism for 
[Spring](https://spring.io) based projects. All Jobs are
configured and managed with a database table. 

## Features

- All Jobs configured in one database table.
- Jobs can run in multithreaded or clustered environments. 
  Synchronisation is done through the database.
- Sentinel feature to detect hanging or killed Jobs.
- Job implementations are simple Spring Beans
- Various RDBMS supported through JPA
- There is also a plain JDBC variant [jobscheduler-jdbc](jobscheduler-jdbc) available.

## Job Configuration Table

| Field           | Type     | Description |
| --------------- | -------- | ----------- |
| NAME            | VARCHAR  | Unique Name of Job (Primary Key) |
| CRON_EXPRESSION | VARCHAR  | 6 Fields separated by space: second minute hour day month weekday. See [Quartz CronExpression](http://www.quartz-scheduler.org/api/2.3.0/org/quartz/CronExpression.html) |
| IMPLEMENTATION  | VARCHAR  | Spring Bean name of Job implentation |
| NEXT_RUN        | TIMESTAMP| Time for next run, evaluated from cron expression or schedule on every run |
| SCHEDULE        | VARCHAR  | Optional: bean name for schedule. Default: CronSchedule |
| PARAMS          | VARCHAR  | Optional: Parameters for Job implementation |
| RUNNING         | BOOLEAN  | Set if Job is running |
| SUSPENDED       | BOOLEAN  | Set to temp. suspend Job |
| DISABLED        | BOOLEAN  | Set to permanently disable Job |
| LAST_EXECUTION_ID| NUMBER  | Reference to last execution |
| ERROR_MAIL_ADDRESS|VARCHAR | Optional, not used by Jobscheduler. May be used by JobLifecycleCallback |

Every Job execution is logged in `JOB_EXECUTION` Table

## Installation

Include maven dependency in your pom.xml:

   ```
       <dependency>
           <groupId>de.laetsch-it.jobscheduler</groupId>
           <artifactId>jobscheduler</artifactId>
           <version>1.2.1</version>
       </dependency>
   ```

#### Spring Boot applications
Just annotate your main application class with `@EnableJobScheduler`

#### Other Spring applications
- Include `de.lit.jobscheduler.JobSchedulerAutoConfiguration` in your Spring Context
- Add two Entity classes from `de.lit.jobscheduler.entity` package to your persistance unit: 
  `JobDefinition`, `JobExecution`

### Configuration options

| Property name                                  | Type    | Description                            |
| ---------------------------------------------- | ------- | -------------------------------------- |
| application.jobscheduler.enable                | Boolean | Disable jobscheduler completely with this set to false. Useful for tests. Default: true |
| application.jobscheduler.initialDelay          | Long    | Initial delay in milliseconds until jobscheduler starts scanning job table and executing jobs. Default: 10000 |
| application.jobscheduler.runinterval           | Long    | Interval in milliseconds for scanning job table for next due run. Default: 20000 |
| application.jobexecutor.corepoolsize           | Integer | Minimum number of threads kept alive in the thread execution pool. Default: 1 |
| application.jobexecutor.maxpoolsize            | Integer | Max number of threads in the thread execution pool. Default: 4 |
| application.jobscheduler.sentinel.enable       | Boolean | Enable sentinel feature. Default: false |
| application.jobscheduler.sentinel.graceperiod  | Long    | Period in minutes until a sign_of_life is considered old (dead). Default: 15 |
| application.jobscheduler.sentinel.update.initial | Long  | Initial delay in milliseconds until sentinel starts updating sign_of_life. Default: 60000 |
| application.jobscheduler.sentinel.update.interval | Long | Interval in milliseconds for updating sign_of_life. Default: 20000 |
| application.jobscheduler.sentinel.initialDelay | Long    | Initial delay in milliseconds until sentinel starts checking for hanging jobs. Default: 70000 |
| application.jobscheduler.sentinel.runinterval  | Long    | Interval in milliseconds for sentinel to check for hanging Jobs. Default: 900000 (15m) |

### Sentinel feature

If the sentinel feature is enabled then two things happen:

1. For every currently running job the `sign-of-life-time` (in Table JOB_EXECUTION) is updated at 
   the configured interval (default 20 seconds).
2. The sentinel service checks regularly if a job execution is marked as `RUNNING` but the `sign-of-life-time`
   is too old (grace period default 15 minutes). If such a job is found then this job will be marked as
   `ABORTED`, all lifecycle hooks called and reset to *not running*. The job can then run again at its
   scheduled time.

This works perfectly well in clustered environments. If one node gets stuck, for instance because of
an *out of memory* exception or JVM crash, then the other nodes will detect and reset all jobs 'hanging'
in the failing instance.

