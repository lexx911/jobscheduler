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
