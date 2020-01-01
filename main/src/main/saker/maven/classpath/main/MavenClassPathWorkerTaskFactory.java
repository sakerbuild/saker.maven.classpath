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
package saker.maven.classpath.main;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
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
import saker.build.util.property.IDEConfigurationRequiredExecutionProperty;
import saker.java.compiler.api.classpath.ClassPathReference;
import saker.maven.support.api.ArtifactCoordinates;
import saker.maven.support.api.MavenOperationConfiguration;
import saker.maven.support.api.localize.ArtifactLocalizationTaskOutput;
import saker.maven.support.api.localize.ArtifactLocalizationUtils;
import saker.maven.support.api.localize.ArtifactLocalizationWorkerTaskOutput;
import saker.std.api.file.location.LocalFileLocation;

public class MavenClassPathWorkerTaskFactory
		implements TaskFactory<ClassPathReference>, Task<ClassPathReference>, Externalizable, TaskIdentifier {
	private static final long serialVersionUID = 1L;

	private MavenOperationConfiguration configuration;
	private Set<ArtifactCoordinates> coordinates;

	/**
	 * For {@link Externalizable}.
	 */
	public MavenClassPathWorkerTaskFactory() {
	}

	public MavenClassPathWorkerTaskFactory(MavenOperationConfiguration configuration,
			Set<ArtifactCoordinates> coordinates) {
		this.configuration = configuration;
		this.coordinates = coordinates;
	}

	@Override
	public Task<? extends ClassPathReference> createTask(ExecutionContext executioncontext) {
		return this;
	}

	@Override
	public ClassPathReference run(TaskContext taskcontext) throws Exception {
		taskcontext.setStandardOutDisplayIdentifier(MavenClassPathTaskFactory.TASK_NAME);

		TaskFactory<? extends ArtifactLocalizationTaskOutput> localizetaskfactory = ArtifactLocalizationUtils
				.createLocalizeArtifactsTaskFactory(configuration, coordinates);
		TaskIdentifier dltaskid = ArtifactLocalizationUtils.createLocalizeArtifactsTaskIdentifier(configuration,
				coordinates);

		TaskIdentifier sourcedltaskid;
		if (taskcontext.getTaskUtilities()
				.getReportExecutionDependency(IDEConfigurationRequiredExecutionProperty.INSTANCE)) {
			Set<ArtifactCoordinates> sourcedlcoordinates = new LinkedHashSet<>();
			for (ArtifactCoordinates dlacoords : coordinates) {
				ArtifactCoordinates sourceacoords = MavenClassPathTaskFactory
						.createSourceArtifactCoordinates(dlacoords);

				sourcedlcoordinates.add(sourceacoords);
			}
			TaskFactory<? extends ArtifactLocalizationTaskOutput> sourcedltaskfactory = ArtifactLocalizationUtils
					.createLocalizeArtifactsTaskFactory(configuration, sourcedlcoordinates);
			sourcedltaskid = ArtifactLocalizationUtils.createLocalizeArtifactsTaskIdentifier(configuration,
					sourcedlcoordinates);
			taskcontext.startTask(sourcedltaskid, sourcedltaskfactory, null);
		} else {
			sourcedltaskid = null;
		}
		ArtifactLocalizationTaskOutput localizeoutput = taskcontext.getTaskUtilities().runTaskResult(dltaskid,
				localizetaskfactory);

		Collection<MavenClassPathEntry> entries = new LinkedHashSet<>();
		StructuredListTaskResult dlresultslist = localizeoutput.getLocalizationResults();
		Iterator<? extends StructuredTaskResult> it = dlresultslist.resultIterator();
		while (it.hasNext()) {
			ArtifactLocalizationWorkerTaskOutput dlres = (ArtifactLocalizationWorkerTaskOutput) it.next()
					.toResult(taskcontext);
			MavenClassPathEntry cpentry = new MavenClassPathEntry(LocalFileLocation.create(dlres.getLocalPath()),
					dlres.getContentDescriptor());
			if (sourcedltaskid != null) {
				ArtifactCoordinates sourceacoords = MavenClassPathTaskFactory
						.createSourceArtifactCoordinates(dlres.getCoordinates());
				cpentry.setSourceAttachment(
						new SourceAttachmentRetrievingStructuredTaskResult(sourcedltaskid, sourceacoords));
			}
			entries.add(cpentry);
		}

		MavenClassPathReference result = new MavenClassPathReference(entries);
		taskcontext.reportSelfTaskOutputChangeDetector(new EqualityTaskOutputChangeDetector(result));
		return result;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(configuration);
		SerialUtils.writeExternalCollection(out, coordinates);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		configuration = (MavenOperationConfiguration) in.readObject();
		coordinates = SerialUtils.readExternalImmutableLinkedHashSet(in);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((configuration == null) ? 0 : configuration.hashCode());
		result = prime * result + ((coordinates == null) ? 0 : coordinates.hashCode());
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
		if (coordinates == null) {
			if (other.coordinates != null)
				return false;
		} else if (!coordinates.equals(other.coordinates))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[]";
	}

}
