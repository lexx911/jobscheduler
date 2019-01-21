package de.lit.jobscheduler;

import java.util.Date;

public class RetryLaterException extends Exception {

	private Date when;

	public RetryLaterException() {
	}

	public RetryLaterException(long delayInMs) {
		this.when = new Date(System.currentTimeMillis() + delayInMs);
	}

	public RetryLaterException(long delayInMs, String message) {
		super(message);
		this.when = new Date(System.currentTimeMillis() + delayInMs);
	}

	public RetryLaterException(Date when, String message) {
		super(message);
		this.when = when;
	}

	public Date getWhen() {
		return when;
	}
}
