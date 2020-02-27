package saker.maven.classpath.impl.option;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import saker.build.task.utils.StructuredTaskResult;

public class MavenClassPathEntryInput implements Externalizable {
	private static final long serialVersionUID = 1L;

	private MavenClassPathInputOption input;
	private StructuredTaskResult implementationVersionKey;
	private MavenClassPathInputOption sourceAttachment;
	private MavenClassPathInputOption documentationAttachment;

	/**
	 * For {@link Externalizable}.
	 */
	public MavenClassPathEntryInput() {
	}

	public MavenClassPathEntryInput(MavenClassPathInputOption input, StructuredTaskResult implementationVersionKey,
			MavenClassPathInputOption sourceAttachment, MavenClassPathInputOption documentationAttachment) {
		this.input = input;
		this.implementationVersionKey = implementationVersionKey;
		this.sourceAttachment = sourceAttachment;
		this.documentationAttachment = documentationAttachment;
	}

	public MavenClassPathInputOption getInput() {
		return input;
	}

	public StructuredTaskResult getImplementationVersionKey() {
		return implementationVersionKey;
	}

	public MavenClassPathInputOption getSourceAttachment() {
		return sourceAttachment;
	}

	public MavenClassPathInputOption getDocumentationAttachment() {
		return documentationAttachment;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(input);
		out.writeObject(implementationVersionKey);
		out.writeObject(sourceAttachment);
		out.writeObject(documentationAttachment);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		input = (MavenClassPathInputOption) in.readObject();
		implementationVersionKey = (StructuredTaskResult) in.readObject();
		sourceAttachment = (MavenClassPathInputOption) in.readObject();
		documentationAttachment = (MavenClassPathInputOption) in.readObject();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((documentationAttachment == null) ? 0 : documentationAttachment.hashCode());
		result = prime * result + ((implementationVersionKey == null) ? 0 : implementationVersionKey.hashCode());
		result = prime * result + ((input == null) ? 0 : input.hashCode());
		result = prime * result + ((sourceAttachment == null) ? 0 : sourceAttachment.hashCode());
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
		MavenClassPathEntryInput other = (MavenClassPathEntryInput) obj;
		if (documentationAttachment == null) {
			if (other.documentationAttachment != null)
				return false;
		} else if (!documentationAttachment.equals(other.documentationAttachment))
			return false;
		if (implementationVersionKey == null) {
			if (other.implementationVersionKey != null)
				return false;
		} else if (!implementationVersionKey.equals(other.implementationVersionKey))
			return false;
		if (input == null) {
			if (other.input != null)
				return false;
		} else if (!input.equals(other.input))
			return false;
		if (sourceAttachment == null) {
			if (other.sourceAttachment != null)
				return false;
		} else if (!sourceAttachment.equals(other.sourceAttachment))
			return false;
		return true;
	}

}
