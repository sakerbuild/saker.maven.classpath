package saker.maven.classpath.impl.option;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import saker.build.task.utils.StructuredTaskResult;

public class TaskResultMavenClassPathInputOption implements MavenClassPathInputOption, Externalizable {
	private static final long serialVersionUID = 1L;

	private StructuredTaskResult taskResult;

	/**
	 * For {@link Externalizable}.
	 */
	public TaskResultMavenClassPathInputOption() {
	}

	public TaskResultMavenClassPathInputOption(StructuredTaskResult taskresult) {
		this.taskResult = taskresult;
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.visit(taskResult);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(taskResult);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		taskResult = (StructuredTaskResult) in.readObject();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((taskResult == null) ? 0 : taskResult.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TaskResultMavenClassPathInputOption other = (TaskResultMavenClassPathInputOption) obj;
		if (taskResult == null) {
			if (other.taskResult != null)
				return false;
		} else if (!taskResult.equals(other.taskResult))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + taskResult + "]";
	}

}
