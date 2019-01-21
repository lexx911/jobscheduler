
CREATE TABLE JOB (
   NAME               VARCHAR2(50 CHAR),
   CRON_EXPRESSION    VARCHAR2(50 CHAR),
   IMPLEMENTATION     VARCHAR2(255 CHAR),
   NEXT_RUN           DATE,
   "TRIGGER"          VARCHAR2(255 CHAR),
   PARAMS             VARCHAR2(4000 CHAR),
   RUNNING            NUMBER(1, 0) DEFAULT 0 NOT NULL,
   SUSPENDED          NUMBER(1, 0) DEFAULT 0 NOT NULL,
   DISABLED           NUMBER(1, 0) DEFAULT 0 NOT NULL,
   LAST_EXECUTION_ID  NUMBER,
   ERROR_MAIL_ADDRESS VARCHAR2(255 CHAR),
   PRIMARY KEY (NAME)
);

CREATE TABLE JOB_EXECUTION (
   ID         NUMBER NOT NULL,
   JOB_NAME   VARCHAR2(50 CHAR) NOT NULL,
   STATUS     VARCHAR2(20 CHAR),
   START_TIME TIMESTAMP(6),
   END_TIME   TIMESTAMP(6),
   MESSAGE    VARCHAR2(4000 CHAR),
   NODE_NAME  VARCHAR2(255 CHAR),
   PRIMARY KEY (ID)
);

alter table JOB add
   constraint FK_JOB_LAST_EXECUTION
      foreign key (LAST_EXECUTION_ID) references job_execution on delete set null;

alter table JOB_EXECUTION add
   constraint FK_EXECUTION_JOB
      foreign key (JOB_NAME) references job on delete cascade;

create sequence job_execution_seq start with 1 increment by 1;
