ALTER TABLE job
   DROP CONSTRAINT fk_job_last_execution;

ALTER TABLE job
   ADD CONSTRAINT fk_job_last_execution
      FOREIGN KEY (last_execution_id) REFERENCES job_execution
         ON DELETE SET NULL;
