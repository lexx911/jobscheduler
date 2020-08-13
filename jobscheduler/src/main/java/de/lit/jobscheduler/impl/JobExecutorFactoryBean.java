// Copyright (c) 2019 Alexander Lätsch, Lätsch IT Consulting GmbH
// This code is licensed under MIT license (see LICENSE.txt for details)

package de.lit.jobscheduler.impl;

import de.lit.jobscheduler.JobLifecycleCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ExecutorConfigurationSupport;

import java.util.concurrent.*;

@Configuration
public class JobExecutorFactoryBean extends ExecutorConfigurationSupport {

	@Value("${application.jobexecutor.corepoolsize:1}")
	private int corePoolSize = 1;

	@Value("${application.jobexecutor.maxpoolsize:4}")
	private int maxPoolSize = 4;

	private int keepAliveSeconds = 60;

	private boolean allowCoreThreadTimeOut = false;

	private int queueCapacity = 0;

	private JobExecutorImpl instance = null;

	private JobLifecycleCallback lifecycleCallback;

	@Override
	protected synchronized ExecutorService initializeExecutor(ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {
		BlockingQueue<Runnable> queue = createQueue(this.queueCapacity);
		instance = new JobExecutorImpl(corePoolSize, maxPoolSize,
				keepAliveSeconds, TimeUnit.SECONDS, queue, threadFactory, rejectedExecutionHandler);
		if (this.allowCoreThreadTimeOut) {
			instance.allowCoreThreadTimeOut(true);
		}
		if (lifecycleCallback != null) {
			instance.setLifecycleCallback(lifecycleCallback);
		}
		return instance;
	}

	private BlockingQueue<Runnable> createQueue(int queueCapacity) {
		if (queueCapacity > 0) {
			return new LinkedBlockingQueue<>(queueCapacity);
		} else {
			return new SynchronousQueue<>();
		}
	}

	@Bean
	public JobExecutor jobExecutor() {
		if (getThreadNamePrefix().startsWith(JobExecutorFactoryBean.class.getName())) {
			setThreadNamePrefix("jobExecutor-");
		}
		return instance;
	}

	@Override
	@Value("jobExecutor-")
	public void setThreadNamePrefix(String threadNamePrefix) {
		super.setThreadNamePrefix(threadNamePrefix);
	}

	/**
	 * @param corePoolSize The ThreadPoolExecutor's core pool size. Default is 1.
	 */
	public void setCorePoolSize(int corePoolSize) {
		this.corePoolSize = corePoolSize;
	}

	/**
	 * @param maxPoolSize The ThreadPoolExecutor's maximum pool size. Default is 4.
	 */
	public void setMaxPoolSize(int maxPoolSize) {
		this.maxPoolSize = maxPoolSize;
	}

	/**
	 * @param keepAliveSeconds The ThreadPoolExecutor's keep-alive seconds. Default is 60.
	 */
	public void setKeepAliveSeconds(int keepAliveSeconds) {
		this.keepAliveSeconds = keepAliveSeconds;
	}

	/**
	 * Specify whether to allow core threads to time out. This enables dynamic
	 * growing and shrinking even in combination with a non-zero queue (since
	 * the max pool size will only grow once the queue is full).
	 *
	 * @param allowCoreThreadTimeOut Boolean. Default is "false".
	 * @see java.util.concurrent.ThreadPoolExecutor#allowCoreThreadTimeOut(boolean)
	 */
	public void setAllowCoreThreadTimeOut(boolean allowCoreThreadTimeOut) {
		this.allowCoreThreadTimeOut = allowCoreThreadTimeOut;
	}

	/**
	 * Set the capacity for the ThreadPoolExecutor's BlockingQueue.
	 * <p>
	 * <b>Please note:</b> The Job Executor schould not be used with a queue because
	 * all queued Jobs would be marked as <i>running</i> in the job table although
	 * they are queued and waiting. If the job executor is occupied then further jobs
	 * will be rejected to run but may be executed on a different node or at a later
	 * time.
	 *
	 * @param queueCapacity capacity Default is {@code 0}.
	 * @see java.util.concurrent.LinkedBlockingQueue
	 * @see java.util.concurrent.SynchronousQueue
	 */
	public void setQueueCapacity(int queueCapacity) {
		this.queueCapacity = queueCapacity;
	}

	public JobLifecycleCallback getLifecycleCallback() {
		return lifecycleCallback;
	}

	@Autowired(required = false)
	public void setLifecycleCallback(JobLifecycleCallback lifecycleCallback) {
		this.lifecycleCallback = lifecycleCallback;
	}
}
