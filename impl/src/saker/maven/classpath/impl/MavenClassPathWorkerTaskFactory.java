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
package saker.maven.classpath.impl;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import saker.build.runtime.execution.ExecutionContext;
import saker.build.task.Task;
import saker.build.task.TaskContext;
import saker.build.task.TaskFactory;
import saker.build.task.identifier.TaskIdentifier;
import saker.build.task.utils.StructuredListTaskResult;
import saker.build.task.utils.StructuredTaskResult;
import saker.build.task.utils.dependencies.EqualityTaskOutputChangeDetector;
import saker.build.thirdparty.saker.util.io.SerialUtils;
import saker.build.trace.BuildTrace;
import saker.build.util.property.IDEConfigurationRequiredExecutionProperty;
import saker.java.compiler.api.classpath.ClassPathReference;
import saker.maven.classpath.impl.option.LiteralStructuredTaskResult;
import saker.maven.classpath.impl.option.MavenClassPathEntryInput;
import saker.maven.classpath.impl.option.MavenClassPathInputOption;
import saker.maven.classpath.main.MavenClassPathTaskFactory;
import saker.maven.support.api.ArtifactCoordinates;
import saker.maven.support.api.MavenOperationConfiguration;
import saker.maven.support.api.localize.ArtifactLocalizationTaskOutput;
import saker.maven.support.api.localize.ArtifactLocalizationUtils;
import saker.maven.support.api.localize.ArtifactLocalizationWorkerTaskOutput;
import saker.std.api.file.location.FileLocation;
import saker.std.api.file.location.LocalFileLocation;

public class MavenClassPathWorkerTaskFactory
		implements TaskFactory<ClassPathReference>, Task<ClassPathReference>, Externalizable, TaskIdentifier {

	private static final long serialVersionUID = 1L;

	private MavenOperationConfiguration configuration;
	private Set<MavenClassPathEntryInput> inputs;

	/**
	 * For {@link Externalizable}.
	 */
	public MavenClassPathWorkerTaskFactory() {
	}

	public MavenClassPathWorkerTaskFactory(MavenOperationConfiguration configuration,
			Set<MavenClassPathEntryInput> input) {
		if (configuration == null) {
			configuration = MavenOperationConfiguration.defaults();
		}
		this.configuration = configuration;
		this.inputs = input;
	}

	@Override
	public Task<? extends ClassPathReference> createTask(ExecutionContext executioncontext) {
		return this;
	}

	@Override
	public ClassPathReference run(TaskContext taskcontext) throws Exception {
		if (saker.build.meta.Versions.VERSION_FULL_COMPOUND >= 8_006) {
			BuildTrace.classifyTask(BuildTrace.CLASSIFICATION_CONFIGURATION);
		}
		taskcontext.setStandardOutDisplayIdentifier(MavenClassPathTaskFactory.TASK_NAME);

		Set<ArtifactCoordinates> coordinputs = new LinkedHashSet<>();
		Set<ArtifactCoordinates> sourcedlcoordinputs = new LinkedHashSet<>();

		for (MavenClassPathEntryInput in : inputs) {
			in.getInput().accept(new ArtifactCoordinateCollectorVisitor(coordinputs));
			MavenClassPathInputOption srcattachment = in.getSourceAttachment();
			if (srcattachment != null) {
				srcattachment.accept(new ArtifactCoordinateCollectorVisitor(sourcedlcoordinputs));
			}
		}

		TaskFactory<? extends ArtifactLocalizationTaskOutput> localizetaskfactory = ArtifactLocalizationUtils
				.createLocalizeArtifactsTaskFactory(configuration, coordinputs);
		TaskIdentifier dltaskid = ArtifactLocalizationUtils.createLocalizeArtifactsTaskIdentifier(configuration,
				coordinputs);

		TaskIdentifier sourcedltaskid;
		if (taskcontext.getTaskUtilities()
				.getReportExecutionDependency(IDEConfigurationRequiredExecutionProperty.INSTANCE)) {
			TaskFactory<? extends ArtifactLocalizationTaskOutput> sourcedltaskfactory = ArtifactLocalizationUtils
					.createLocalizeArtifactsTaskFactory(configuration, sourcedlcoordinputs);
			sourcedltaskid = ArtifactLocalizationUtils.createLocalizeArtifactsTaskIdentifier(configuration,
					sourcedlcoordinputs);
			taskcontext.startTask(sourcedltaskid, sourcedltaskfactory, null);
		} else {
			sourcedltaskid = null;
		}
		ArtifactLocalizationTaskOutput localizeoutput = taskcontext.getTaskUtilities().runTaskResult(dltaskid,
				localizetaskfactory);

		Map<ArtifactCoordinates, ArtifactLocalizationWorkerTaskOutput> coordinatelocalizationoutputs = new HashMap<>();
		StructuredListTaskResult dlresultslist = localizeoutput.getLocalizationResults();
		Iterator<? extends StructuredTaskResult> it = dlresultslist.resultIterator();
		while (it.hasNext()) {
			ArtifactLocalizationWorkerTaskOutput dlres = (ArtifactLocalizationWorkerTaskOutput) it.next()
					.toResult(taskcontext);
			coordinatelocalizationoutputs.put(dlres.getCoordinates(), dlres);
		}

		Collection<MavenClassPathEntry> entries = new LinkedHashSet<>();
		for (MavenClassPathEntryInput in : inputs) {
			MavenClassPathEntry entry = new MavenClassPathEntry();
			in.getInput().accept(new MavenClassPathInputOption.Visitor() {
				@Override
				public void visit(StructuredTaskResult taskresult) {
					Object res = taskresult.toResult(taskcontext);
					if (res == null) {
						//don't include this class path entry in the result
						return;
					}
					if (res instanceof FileLocation) {
						entry.setFileLocation((FileLocation) res);
						return;
					}
					// XXX support more input types
					throw new UnsupportedOperationException("unsupported class path entry type: " + res);
				}

				@Override
				public void visit(FileLocation file) {
					entry.setFileLocation(file);
				}

				@Override
				public void visit(ArtifactCoordinates artifact) {
					ArtifactLocalizationWorkerTaskOutput dlres = coordinatelocalizationoutputs.get(artifact);
					if (dlres == null) {
						throw new RuntimeException("Failed to localize classpath artifact: " + artifact);
					}
					entry.setFileLocation(LocalFileLocation.create(dlres.getLocalPath()));
					entry.setImplementationVersionKey(dlres.getContentDescriptor());
				}
			});
			if (!entry.hasInput()) {
				//input not set, don't include the entry
				continue;
			}
			if (entry.getImplementationVersionKey() == null) {
				StructuredTaskResult inimplkey = in.getImplementationVersionKey();
				if (inimplkey != null) {
					entry.setImplementationVersionKey(inimplkey.toResult(taskcontext));
				}
			}
			MavenClassPathInputOption srcattachment = in.getSourceAttachment();
			if (srcattachment != null) {
				srcattachment.accept(new MavenClassPathInputOption.Visitor() {
					@Override
					public void visit(StructuredTaskResult taskresult) {
						entry.setSourceAttachment(taskresult);
					}

					@Override
					public void visit(FileLocation file) {
						entry.setSourceAttachment(new LiteralStructuredTaskResult(file));
					}

					@Override
					public void visit(ArtifactCoordinates artifact) {
						entry.setSourceAttachment(
								new SourceAttachmentRetrievingStructuredTaskResult(sourcedltaskid, artifact));
					}
				});
			}
			entries.add(entry);
		}

		MavenClassPathReference result = new MavenClassPathReference(entries);
		taskcontext.reportSelfTaskOutputChangeDetector(new EqualityTaskOutputChangeDetector(result));
		return result;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(configuration);
		SerialUtils.writeExternalCollection(out, inputs);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		configuration = (MavenOperationConfiguration) in.readObject();
		inputs = SerialUtils.readExternalImmutableLinkedHashSet(in);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((configuration == null) ? 0 : configuration.hashCode());
		result = prime * result + ((inputs == null) ? 0 : inputs.hashCode());
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
		MavenClassPathWorkerTaskFactory other = (MavenClassPathWorkerTaskFactory) obj;
		if (configuration == null) {
			if (other.configuration != null)
				return false;
		} else if (!configuration.equals(other.configuration))
			return false;
		if (inputs == null) {
			if (other.inputs != null)
				return false;
		} else if (!inputs.equals(other.inputs))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[]";
	}

	public static ArtifactCoordinates createSourceArtifactCoordinates(ArtifactCoordinates dlacoords) {
		//always expect the sources to be in an artifact with "jar" extension
		//    e.g. for aar (android libs) artifacts, the sources are still in a "jar" artifact, so using the same 
		//         extension will fail
		ArtifactCoordinates sourceacoords = new ArtifactCoordinates(dlacoords.getGroupId(), dlacoords.getArtifactId(),
				"sources", "jar", dlacoords.getVersion());
		return sourceacoords;
	}

	private static final class ArtifactCoordinateCollectorVisitor implements MavenClassPathInputOption.Visitor {
		private final Set<ArtifactCoordinates> coordinputs;

		private ArtifactCoordinateCollectorVisitor(Set<ArtifactCoordinates> coordinputs) {
			this.coordinputs = coordinputs;
		}

		@Override
		public void visit(StructuredTaskResult taskresult) {
		}

		@Override
		public void visit(FileLocation file) {
		}

		@Override
		public void visit(ArtifactCoordinates artifact) {
			coordinputs.add(artifact);
		}
	}
}
