update job set last_execution_id=null;
delete from job_execution;
delete from job;

insert into job(name, implementation, cron_expression, next_run, running, disabled, suspended, run_queue)
values ('TESTJOB1', 'testjob1', '0 0 0 1 1 ?', '2000-01-01 12:00:00', 0, 0, 1, 'q1');

insert into job(name, implementation, cron_expression, next_run, running, disabled, suspended, run_queue)
values ('TESTJOB2', 'testjob2', '0 0 0 1 1 ?', '2000-01-01 12:00:00', 0, 0, 1, 'q1');
