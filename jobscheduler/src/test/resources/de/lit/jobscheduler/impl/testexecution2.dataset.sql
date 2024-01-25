update job set last_execution_id=null;
delete from job_execution;
delete from job;

insert into job(name, implementation, cron_expression, next_run, running, disabled, suspended)
values ('TESTJOB1', 'testjob1', '0 0 0 1 1 ?', '2990-01-01 12:00:00', 1, 0, 0);

insert into job_execution(id, job_name, start_time, status, node_name, sign_of_life_time)
values ('100', 'TESTJOB1', '2020-01-01 12:05:00', 'RUNNING', 'testnode', '2020-01-01 12:15:25');
