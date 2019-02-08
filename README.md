# Database Job Scheduler

## Overview
This library provides a job scheduling mechanism for 
[Spring](https://spring.io) based projects. All Jobs are
configured and managed with a database table. 

## Features

- All Jobs configured in one database table.
- Jobs can run in multithreaded or clustered environments. 
  Synchronisation is done through the database.
- Job implementations are simple Spring Beans
- Various RDBMS supported through JPA

## Job Configuration Table

| Field           | Type     | Description |
| --------------- | -------- | ----------- |
| NAME            | VARCHAR  | Unique Name of Job (Primary Key) |
| CRON_EXPRESSION | VARCHAR  | 6 Fields separated by space: second minute hour day month weekday |
| IMPLEMENTATION  | VARCHAR  | Spring Bean name of Job implentation |
| NEXT_RUN        | TIMESTAMP| Time for next run, evaluated from cron expression on every run |
| TRIGGER         | VARCHAR  | Optional: bean name for trigger expression |
| PARAMS          | VARCHAR  | Optional: Parameters for Job implementation |
| RUNNING         | BOOLEAN  | Set if Job is running |
| SUSPENDED       | BOOLEAN  | Set to temp. suspend Job |
| DISABLED        | BOOLEAN  | Set to permanently disable Job |
| LAST_EXECUTION_ID|NUMBER   | Refernce to last execution |
| ERROR_MAIL_ADDRESS|VARCHAR | Optional, not used by Jobscheduler |

Every Job execution is logged in `JOB_EXECUTION` Table

## Installation

Include maven dependency in your pom.xml:

   ```
       <dependency>
           <groupId>de.laetsch-it.jobscheduler</groupId>
           <artifactId>jobscheduler</artifactId>
           <version>1.0.2</version>
       </dependency>
   ```
#### Spring Boot applications
Just annotate your main application class with `@EnableJobScheduler`

#### Other Spring applications
- Include `de.lit.jobscheduler.JobSchedulerAutoConfiguration` in your Spring Context
- Add two Entity classes from `de.lit.jobscheduler.entity` package to your persistance unit: 
  `JobDefinition`, `JobExecution`
