package de.lit.jobscheduler;

import java.util.ArrayList;
import java.util.List;

public class MultipleErrorException extends RuntimeException {
	private final List<Throwable> causes = new ArrayList<>();

	public MultipleErrorException() {
		super();
	}

	public MultipleErrorException(String message, Throwable cause) {
		super(message, cause);
		causes.add(cause);
	}

	public MultipleErrorException(String message) {
		super(message);
	}

	public MultipleErrorException(Throwable cause) {
		super(cause);
		causes.add(cause);
	}

	public MultipleErrorException addIfNotNull(Throwable error) {
		if (error != null)
			causes.add(error);
		return this;
	}

	public boolean isEmpty() {
		return causes.isEmpty();
	}

	public void throwIfNotEmpty() {
		if (!causes.isEmpty()) {
			throw this;
		}
	}

	public List<Throwable> getCauses() {
		return causes;
	}

	@Override
	public String getMessage() {
		StringBuilder out = new StringBuilder(super.getMessage());
		for (Throwable throwable : causes) {
			out.append("\n").append(throwable.toString());
			if (throwable.getCause() != null) {
				out.append(" Caused by:").append(throwable.getCause().toString());
			}
		}
		return out.toString();
	}
}
