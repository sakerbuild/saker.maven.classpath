/*
 * Copyright (C) 2020 Bence Sipka
 *
 * This program is free software: you can redistribute it and/or modify 
 * it under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package saker.maven.classpath.api;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import saker.build.task.TaskFactory;
import saker.build.task.identifier.TaskIdentifier;
import saker.build.task.utils.StructuredTaskResult;
import saker.java.compiler.api.classpath.ClassPathReference;
import saker.maven.classpath.impl.MavenClassPathWorkerTaskFactory;
import saker.maven.classpath.impl.option.ArtifactMavenClassPathInputOption;
import saker.maven.classpath.impl.option.FileMavenClassPathInputOption;
import saker.maven.classpath.impl.option.LiteralStructuredTaskResult;
import saker.maven.classpath.impl.option.MavenClassPathEntryInput;
import saker.maven.classpath.impl.option.MavenClassPathInputOption;
import saker.maven.support.api.ArtifactCoordinates;
import saker.maven.support.api.MavenOperationConfiguration;
import saker.std.api.file.location.FileLocation;

public class MavenClassPathTaskBuilder {
	private MavenOperationConfiguration configuration;
	private Set<MavenClassPathEntryInput> inputs = new LinkedHashSet<>();

	public void setConfiguration(MavenOperationConfiguration configuration) {
		this.configuration = configuration;
	}

	public MavenClassPathTaskBuilder add(MavenClassPathTaskBuilder.EntryBuilder builder) {
		MavenClassPathInputOption input = Objects.requireNonNull(builder.input, "input");
		inputs.add(new MavenClassPathEntryInput(input, builder.implementationVersionKey, builder.sourceAttachment,
				builder.documentationAttachment));
		return this;
	}

	public TaskFactory<? extends ClassPathReference> buildTask() {
		return new MavenClassPathWorkerTaskFactory(configuration, inputs);
	}

	public TaskIdentifier buildTaskIdentifier() {
		return new MavenClassPathWorkerTaskFactory(configuration, inputs);
	}

	public static class EntryBuilder {
		private MavenClassPathInputOption input;
		private StructuredTaskResult implementationVersionKey;
		private MavenClassPathInputOption sourceAttachment;
		private MavenClassPathInputOption documentationAttachment;

		public MavenClassPathTaskBuilder.EntryBuilder setInput(FileLocation input) {
			if (input == null) {
				this.input = null;
			} else {
				this.input = new FileMavenClassPathInputOption(input);
			}
			return this;
		}

		public MavenClassPathTaskBuilder.EntryBuilder setInput(ArtifactCoordinates input) {
			if (input == null) {
				this.input = null;
			} else {
				this.input = new ArtifactMavenClassPathInputOption(input);
			}
			return this;
		}

		public MavenClassPathTaskBuilder.EntryBuilder setSourceAttachment(FileLocation sourceAttachment) {
			if (sourceAttachment == null) {
				this.sourceAttachment = null;
			} else {
				this.sourceAttachment = new FileMavenClassPathInputOption(sourceAttachment);
			}
			return this;
		}

		public MavenClassPathTaskBuilder.EntryBuilder setSourceAttachment(ArtifactCoordinates sourceAttachment) {
			if (sourceAttachment == null) {
				this.sourceAttachment = null;
			} else {
				this.sourceAttachment = new ArtifactMavenClassPathInputOption(sourceAttachment);
			}
			return this;
		}

		public MavenClassPathTaskBuilder.EntryBuilder setImplementationVersionKey(StructuredTaskResult implementationVersionKey) {
			this.implementationVersionKey = implementationVersionKey;
			return this;
		}

		public MavenClassPathTaskBuilder.EntryBuilder setImplementationVersionKey(Object implementationVersionKey) {
			if (implementationVersionKey == null) {
				this.implementationVersionKey = null;
			} else {
				this.implementationVersionKey = new LiteralStructuredTaskResult(implementationVersionKey);
			}
			return this;
		}
	}
}