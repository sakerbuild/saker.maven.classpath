package saker.maven.classpath.main;

import saker.nest.scriptinfo.reflection.annot.NestInformation;
import saker.nest.scriptinfo.reflection.annot.NestTypeInformation;

public class TaskDocs {
	private TaskDocs() {
		throw new UnsupportedOperationException();
	}

	@NestTypeInformation(qualifiedName = "ClassPathReference")
	@NestInformation("Classpath object for the specified Maven artifacts.")
	public static class DocArtifactClassPath {
	}
}
