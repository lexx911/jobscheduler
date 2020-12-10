create table job
(
  name               varchar(50) not null,
  cron_expression    varchar(255),
  disabled           boolean     not null,
  error_mail_address varchar(255),
  implementation     varchar(255),
  next_run           timestamp,
  params             varchar(255),
  running            boolean     not null,
  schedule           varchar(255),
  suspended          boolean     not null,
  last_execution_id  bigint,
  primary key (name)
);

create table job_execution
(
  id         bigint generated always as identity not null,
  end_time   TIMESTAMP(6),
  message    varchar(4000),
  node_name  varchar(255),
  start_time TIMESTAMP(6),
  status     varchar(20),
  job_name   varchar(50),
  sign_of_life_time timestamp(6),
  primary key (id)
);

create sequence job_execution_seq start with 1 increment by 1;

alter table job
  add constraint fk_job_last_execution foreign key (last_execution_id) references job_execution ON DELETE SET NULL;

alter table job_execution
  add constraint fk_job_execution_job foreign key (job_name) references job;
