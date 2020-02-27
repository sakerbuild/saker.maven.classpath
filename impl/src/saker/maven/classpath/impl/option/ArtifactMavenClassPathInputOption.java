package saker.maven.classpath.impl.option;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import saker.maven.support.api.ArtifactCoordinates;

public class ArtifactMavenClassPathInputOption implements MavenClassPathInputOption, Externalizable {
	private static final long serialVersionUID = 1L;

	private ArtifactCoordinates artifact;

	/**
	 * For {@link Externalizable}.
	 */
	public ArtifactMavenClassPathInputOption() {
	}

	public ArtifactMavenClassPathInputOption(ArtifactCoordinates artifact) {
		this.artifact = artifact;
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.visit(artifact);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(artifact);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		artifact = (ArtifactCoordinates) in.readObject();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((artifact == null) ? 0 : artifact.hashCode());
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
		ArtifactMavenClassPathInputOption other = (ArtifactMavenClassPathInputOption) obj;
		if (artifact == null) {
			if (other.artifact != null)
				return false;
		} else if (!artifact.equals(other.artifact))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + artifact + "]";
	}

}
