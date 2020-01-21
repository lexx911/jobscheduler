create table job
(
	name varchar(50) not null,
	implementation varchar(255) null,
	params varchar(4000) null,
	cron_expression varchar(50) null,
	next_run datetime null,
	running bit not null,
	disabled bit not null,
	suspended bit not null,
	`schedule` varchar(255) null,
	last_execution_id bigint null,
	error_mail_address varchar(255) null,
	primary key(name)
)
engine=InnoDB;


create table job_execution
(
	id bigint auto_increment,
	job_name varchar(50) null,
	start_time timestamp(6) null default CURRENT_TIMESTAMP(6),
	end_time timestamp(6) null,
	status varchar(20) null,
	message text null,
	node_name varchar(255) null,
  sign_of_life_time timestamp(6) null,
	primary key(id),
	key fk_job_execution_job (job_name),
  constraint fk_job_execution_job
    foreign key (job_name) references job(name) on delete cascade on update cascade
)
engine=InnoDB;

alter table job add constraint fk_job_last_execution
  foreign key (last_execution_id) references job_execution(id) on delete set null;

