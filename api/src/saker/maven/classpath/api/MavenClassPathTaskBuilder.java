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
import saker.build.task.TaskResultResolver;
import saker.build.task.identifier.TaskIdentifier;
import saker.build.task.utils.StructuredTaskResult;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.java.compiler.api.classpath.ClassPathEntry;
import saker.java.compiler.api.classpath.ClassPathReference;
import saker.maven.classpath.impl.MavenClassPathWorkerTaskFactory;
import saker.maven.classpath.impl.option.ArtifactMavenClassPathInputOption;
import saker.maven.classpath.impl.option.FileMavenClassPathInputOption;
import saker.maven.classpath.impl.option.LiteralStructuredTaskResult;
import saker.maven.classpath.impl.option.MavenClassPathEntryInput;
import saker.maven.classpath.impl.option.MavenClassPathInputOption;
import saker.maven.classpath.impl.option.TaskResultMavenClassPathInputOption;
import saker.maven.support.api.ArtifactCoordinates;
import saker.maven.support.api.MavenOperationConfiguration;
import saker.std.api.file.location.FileLocation;

/**
 * Builder class for creating a build task that returns a Maven classpath object.
 * <p>
 * Use {@link #newBuilder()} to create a new instance.
 * 
 * @since saker.maven.classpath 0.8.1
 */
public class MavenClassPathTaskBuilder {
	private MavenOperationConfiguration configuration;
	private Set<MavenClassPathEntryInput> inputs = new LinkedHashSet<>();

	private MavenClassPathTaskBuilder() {
	}

	/**
	 * Creates a new builder instance.
	 * 
	 * @return The created builder.
	 */
	public static MavenClassPathTaskBuilder newBuilder() {
		return new MavenClassPathTaskBuilder();
	}

	/**
	 * Sets the Maven configuration that should be used when managing artifacts.
	 * 
	 * @param configuration
	 *            The configuration or <code>null</code> to use the {@linkplain MavenOperationConfiguration#defaults()
	 *            defaults}.
	 */
	public void setConfiguration(MavenOperationConfiguration configuration) {
		this.configuration = configuration;
	}

	/**
	 * Adds a new classpath entry specified by the given entry builder.
	 * <p>
	 * The argument entry builder can be reused after this call.
	 * 
	 * @param builder
	 *            The entry builder.
	 * @return <code>this</code>
	 * @throws NullPointerException
	 *             If the argument or its input is null.
	 */
	public MavenClassPathTaskBuilder add(MavenClassPathTaskBuilder.EntryBuilder builder) throws NullPointerException {
		Objects.requireNonNull(builder, "entry builder");
		MavenClassPathInputOption input = Objects.requireNonNull(builder.input, "input");
		inputs.add(new MavenClassPathEntryInput(input, builder.implementationVersionKey, builder.sourceAttachment,
				builder.documentationAttachment));
		return this;
	}

	/**
	 * Builds a Maven classpath creator task.
	 * <p>
	 * The task should be started with a task identifier built using {@link #buildTaskIdentifier()}.
	 * <p>
	 * The builder can be reused after this call.
	 * 
	 * @return The created task factory.
	 */
	public TaskFactory<? extends ClassPathReference> buildTask() {
		return new MavenClassPathWorkerTaskFactory(configuration, ImmutableUtils.makeImmutableLinkedHashSet(inputs));
	}

	/**
	 * Builds a task identifier to be used with the {@linkplain #buildTask() built task}.
	 * <p>
	 * The builder can be reused after this call.
	 * 
	 * @return The task identifier.
	 */
	public TaskIdentifier buildTaskIdentifier() {
		return new MavenClassPathWorkerTaskFactory(configuration, ImmutableUtils.makeImmutableLinkedHashSet(inputs));
	}

	/**
	 * A Maven classpath entry builder class.
	 * <p>
	 * The builder can be used to create new classpath entries to be used with {@link MavenClassPathTaskBuilder}. Use
	 * the {@link MavenClassPathTaskBuilder#add(EntryBuilder)} method to add an entry to it.
	 * <p>
	 * Use {@link #newBuilder()} to create a new instance.
	 */
	public static class EntryBuilder {
		private MavenClassPathInputOption input;
		private StructuredTaskResult implementationVersionKey;
		private MavenClassPathInputOption sourceAttachment;
		private MavenClassPathInputOption documentationAttachment;

		private EntryBuilder() {
		}

		/**
		 * Creates a new builder instance.
		 * 
		 * @return The created builder.
		 */
		public static EntryBuilder newBuilder() {
			return new EntryBuilder();
		}

		/**
		 * Sets the input of the classpath.
		 * 
		 * @param input
		 *            The input file.
		 * @return <code>this</code>
		 * @see ClassPathEntry#getInputFile()
		 */
		public EntryBuilder setInput(FileLocation input) {
			if (input == null) {
				this.input = null;
			} else {
				this.input = new FileMavenClassPathInputOption(input);
			}
			return this;
		}

		/**
		 * Sets the input of the classpath.
		 * <p>
		 * The artifact will be resoved with the associated
		 * {@linkplain MavenClassPathTaskBuilder#setConfiguration(MavenOperationConfiguration) Maven configuration}.
		 * 
		 * @param input
		 *            The input artifact coordinates.
		 * @return <code>this</code>
		 * @see ClassPathEntry#getInputFile()
		 */
		public EntryBuilder setInput(ArtifactCoordinates input) {
			if (input == null) {
				this.input = null;
			} else {
				this.input = new ArtifactMavenClassPathInputOption(input);
			}
			return this;
		}

		/**
		 * Sets the input of the classpath.
		 * <p>
		 * The argument task result should resolve to a {@link FileLocation}.
		 * 
		 * @param input
		 *            The input task result.
		 * @return <code>this</code>
		 * @see ClassPathEntry#getInputFile()
		 * @since saker.maven.classpath 0.8.2
		 */
		public EntryBuilder setInput(StructuredTaskResult input) {
			if (input == null) {
				this.input = null;
			} else {
				this.input = new TaskResultMavenClassPathInputOption(input);
			}
			return this;
		}

		/**
		 * Sets the source attachment of the classpath.
		 * 
		 * @param input
		 *            The source attachment file.
		 * @return <code>this</code>
		 * @see ClassPathEntry#getSourceAttachment()
		 */
		public EntryBuilder setSourceAttachment(FileLocation sourceAttachment) {
			if (sourceAttachment == null) {
				this.sourceAttachment = null;
			} else {
				this.sourceAttachment = new FileMavenClassPathInputOption(sourceAttachment);
			}
			return this;
		}

		/**
		 * Sets the source attachment of the classpath.
		 * <p>
		 * The artifact will be resoved with the associated
		 * {@linkplain MavenClassPathTaskBuilder#setConfiguration(MavenOperationConfiguration) Maven configuration}.
		 * 
		 * @param input
		 *            The source attachment artifact coordinates.
		 * @return <code>this</code>
		 * @see ClassPathEntry#getSourceAttachment()
		 */
		public EntryBuilder setSourceAttachment(ArtifactCoordinates sourceAttachment) {
			if (sourceAttachment == null) {
				this.sourceAttachment = null;
			} else {
				this.sourceAttachment = new ArtifactMavenClassPathInputOption(sourceAttachment);
			}
			return this;
		}

		/**
		 * Sets the source attachment of the classpath.
		 * <p>
		 * The argument is the equivalent of setting the result of the {@link ClassPathEntry#getSourceAttachment()}
		 * method.
		 * 
		 * @param input
		 *            The source attachment task result.
		 * @return <code>this</code>
		 * @see ClassPathEntry#getSourceAttachment()
		 * @since saker.maven.classpath 0.8.2
		 */
		public EntryBuilder setSourceAttachment(StructuredTaskResult sourceAttachment) {
			if (sourceAttachment == null) {
				this.sourceAttachment = null;
			} else {
				this.sourceAttachment = new TaskResultMavenClassPathInputOption(sourceAttachment);
			}
			return this;
		}

		/**
		 * Sets the implementation version key of the classpath.
		 * <p>
		 * {@link StructuredTaskResult#toResult(TaskResultResolver)} will be called on the argument by the classpath
		 * creator task.
		 * <p>
		 * If the version key is set to <code>null</code>, it may be automatically inferred by the classpath creator
		 * task.
		 * 
		 * @param implementationVersionKey
		 *            The version key as a structured task result.
		 * @return <code>this</code>
		 */
		public EntryBuilder setImplementationVersionKey(StructuredTaskResult implementationVersionKey) {
			this.implementationVersionKey = implementationVersionKey;
			return this;
		}

		/**
		 * Sets the implementation version key of the classpath.
		 * <p>
		 * If the argument is an instance of {@link StructuredTaskResult}, this method is the same as
		 * {@link #setImplementationVersionKey(StructuredTaskResult)}.
		 * <p>
		 * If the version key is set to <code>null</code>, it may be automatically inferred by the classpath creator
		 * task.
		 * 
		 * @param implementationVersionKey
		 *            The implementation version key.
		 * @return <code>this</code>
		 */
		public EntryBuilder setImplementationVersionKey(Object implementationVersionKey) {
			if (implementationVersionKey == null) {
				this.implementationVersionKey = null;
			} else if (implementationVersionKey instanceof StructuredTaskResult) {
				this.implementationVersionKey = (StructuredTaskResult) implementationVersionKey;
			} else {
				this.implementationVersionKey = new LiteralStructuredTaskResult(implementationVersionKey);
			}
			return this;
		}
	}
}