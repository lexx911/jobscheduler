# Database Job Scheduler

This is the plain JDBC variant of the [jobscheduler](..) library.

## Installation

Include maven dependency in your pom.xml:

    ```
       <dependency>
           <groupId>de.laetsch-it.jobscheduler</groupId>
           <artifactId>jobscheduler-jdbc</artifactId>
           <version>1.2.1</version>
       </dependency>
    ```

## Configuration

This library provides 2 DAO implementations: `JdbcJobDefinitionDao` and
`JdbcJobExecutionDao`. You have to include them into your Spring application 
context and optionally set the tablename and idGenerator properties.
