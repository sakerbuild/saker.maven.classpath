package saker.maven.classpath.impl.option;

import saker.build.task.utils.StructuredTaskResult;
import saker.maven.support.api.ArtifactCoordinates;
import saker.std.api.file.location.FileLocation;

public interface MavenClassPathInputOption {
	public void accept(Visitor visitor);

	public interface Visitor {
		public void visit(ArtifactCoordinates artifact);

		public void visit(FileLocation file);

		public void visit(StructuredTaskResult taskresult);
	}
}
