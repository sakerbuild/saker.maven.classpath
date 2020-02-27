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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import saker.build.runtime.execution.ExecutionContext;
import saker.build.task.ParameterizableTask;
import saker.build.task.TaskContext;
import saker.build.task.TaskFactory;
import saker.build.task.identifier.TaskIdentifier;
import saker.build.task.utils.SimpleStructuredObjectTaskResult;
import saker.build.task.utils.StructuredListTaskResult;
import saker.build.task.utils.StructuredTaskResult;
import saker.build.task.utils.annot.SakerInput;
import saker.build.task.utils.dependencies.EqualityTaskOutputChangeDetector;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.build.trace.BuildTrace;
import saker.build.util.property.IDEConfigurationRequiredExecutionProperty;
import saker.maven.classpath.impl.MavenClassPathEntry;
import saker.maven.classpath.impl.MavenClassPathReference;
import saker.maven.classpath.impl.MavenClassPathWorkerTaskFactory;
import saker.maven.classpath.impl.SourceAttachmentRetrievingStructuredTaskResult;
import saker.maven.classpath.main.TaskDocs.DocArtifactClassPath;
import saker.maven.support.api.ArtifactCoordinates;
import saker.maven.support.api.MavenOperationConfiguration;
import saker.maven.support.api.dependency.MavenDependencyResolutionTaskOutput;
import saker.maven.support.api.dependency.ResolvedDependencyArtifact;
import saker.maven.support.api.download.ArtifactDownloadTaskOutput;
import saker.maven.support.api.download.ArtifactDownloadUtils;
import saker.maven.support.api.download.ArtifactDownloadWorkerTaskOutput;
import saker.maven.support.api.localize.ArtifactLocalizationTaskOutput;
import saker.maven.support.api.localize.ArtifactLocalizationUtils;
import saker.maven.support.api.localize.ArtifactLocalizationWorkerTaskOutput;
import saker.maven.support.main.configuration.option.MavenConfigurationTaskOption;
import saker.maven.support.main.configuration.option.MavenOperationConfigurationTaskOptionUtils;
import saker.nest.scriptinfo.reflection.annot.NestInformation;
import saker.nest.scriptinfo.reflection.annot.NestParameterInformation;
import saker.nest.scriptinfo.reflection.annot.NestTaskInformation;
import saker.nest.scriptinfo.reflection.annot.NestTypeUsage;
import saker.nest.utils.FrontendTaskFactory;
import saker.std.api.file.location.ExecutionFileLocation;
import saker.std.api.file.location.LocalFileLocation;

@NestTaskInformation(returnType = @NestTypeUsage(DocArtifactClassPath.class))
@NestInformation("Creates a class path object that can be used as an input to the saker.java.compile() and similar tasks.\n"
		+ "The task creates a class path representation of the input artifacts.\n"
		+ "If the build execution is configured to generate IDE configurations, the source artifacts will be retrieved from the repositories "
		+ "and added to the created configurations.")
@NestParameterInformation(value = "Artifacts",
		aliases = { "", "Artifact" },
		required = true,
		type = @NestTypeUsage(value = Collection.class,
				elementTypes = saker.maven.support.main.TaskDocs.DocInputArtifactCoordinates.class),
		info = @NestInformation("Specifies the artifact that should be part of the created class path.\n"
				+ "The parameter accepts one or more artifact coordinates, or outputs from Maven dependency resolution, localization "
				+ "or artifact download tasks.\n"
				+ "The artifact coordinates are expected in the <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version> format.\n"))
@NestParameterInformation(value = "Configuration",
		type = @NestTypeUsage(MavenConfigurationTaskOption.class),
		info = @NestInformation(saker.maven.support.main.TaskDocs.PARAM_CONFIGURATION))
public class MavenClassPathTaskFactory extends FrontendTaskFactory<Object> {
	private static final long serialVersionUID = 1L;

	public static final String TASK_NAME = "saker.maven.classpath";

	@Override
	public ParameterizableTask<? extends Object> createTask(ExecutionContext executioncontext) {
		return new ParameterizableTask<Object>() {
			@SakerInput(value = { "", "Artifact", "Artifacts" }, required = true)
			public Object artifacts;

			@SakerInput(value = { "Configuration" })
			public MavenConfigurationTaskOption configuration;

			@Override
			public Object run(TaskContext taskcontext) throws Exception {
				if (saker.build.meta.Versions.VERSION_FULL_COMPOUND >= 8_006) {
					BuildTrace.classifyTask(BuildTrace.CLASSIFICATION_FRONTEND);
				}

				if (artifacts instanceof StructuredTaskResult) {
					if (artifacts instanceof StructuredListTaskResult) {
						StructuredListTaskResult arifactsstructuredlist = (StructuredListTaskResult) artifacts;
						Set<ArtifactCoordinates> coordinates = new LinkedHashSet<>();
						Iterator<? extends StructuredTaskResult> it = arifactsstructuredlist.resultIterator();
						while (it.hasNext()) {
							Object resobj = it.next().toResult(taskcontext);
							String resstr = Objects.toString(resobj, null);
							if (ObjectUtils.isNullOrEmpty(resstr)) {
								continue;
							}
							try {
								coordinates.add(ArtifactCoordinates.valueOf(resstr));
							} catch (IllegalArgumentException e) {
								taskcontext.abortExecution(e);
								return null;
							}
						}
						return handleArtifactCoordinates(taskcontext, getRepositoryOperationConfiguration(),
								coordinates);
					}
					StructuredTaskResult structuredartifacts = (StructuredTaskResult) artifacts;
					artifacts = structuredartifacts.toResult(taskcontext);
				}
				if (artifacts instanceof Object[]) {
					artifacts = ImmutableUtils.makeImmutableList((Object[]) artifacts);
				}
				if (artifacts instanceof Iterable<?>) {
					Iterable<?> artifactsiterable = (Iterable<?>) artifacts;
					Set<ArtifactCoordinates> coordinates = new LinkedHashSet<>();

					for (Object o : artifactsiterable) {
						String coordstr = Objects.toString(o, null);
						if (ObjectUtils.isNullOrEmpty(coordstr)) {
							continue;
						}
						try {
							coordinates.add(ArtifactCoordinates.valueOf(coordstr));
						} catch (IllegalArgumentException e) {
							taskcontext.abortExecution(e);
							return null;
						}
					}
					return handleArtifactCoordinates(taskcontext, getRepositoryOperationConfiguration(), coordinates);
				}

				if (artifacts instanceof ArtifactDownloadTaskOutput) {
					ArtifactDownloadTaskOutput downloadoutput = (ArtifactDownloadTaskOutput) artifacts;
					return handleDownloadOutput(taskcontext, downloadoutput.getConfiguration(), downloadoutput);
				}
				if (artifacts instanceof ArtifactLocalizationTaskOutput) {
					ArtifactLocalizationTaskOutput localizationoutput = (ArtifactLocalizationTaskOutput) artifacts;
					return handleLocalizationOutput(taskcontext, localizationoutput.getConfiguration(),
							localizationoutput);
				}
				if (artifacts instanceof MavenDependencyResolutionTaskOutput) {
					MavenDependencyResolutionTaskOutput depoutput = (MavenDependencyResolutionTaskOutput) artifacts;
					Set<ArtifactCoordinates> coordinates = ImmutableUtils
							.makeImmutableLinkedHashSet(depoutput.getArtifactCoordinates());
					return handleArtifactCoordinates(taskcontext, depoutput.getConfiguration(), coordinates);
				}
				if (artifacts instanceof ResolvedDependencyArtifact) {
					ResolvedDependencyArtifact resolvedartifact = (ResolvedDependencyArtifact) artifacts;
					return handleArtifactCoordinates(taskcontext, resolvedartifact.getConfiguration(),
							ImmutableUtils.singletonSet(resolvedartifact.getCoordinates()));
				}

				String coordsstr = Objects.toString(artifacts, null);
				if (coordsstr == null) {
					NullPointerException npe = new NullPointerException("null Artifacts input argument.");
					taskcontext.abortExecution(npe);
					return null;
				}
				try {
					return handleArtifactCoordinates(taskcontext, getRepositoryOperationConfiguration(),
							Collections.singleton(ArtifactCoordinates.valueOf(coordsstr)));
				} catch (IllegalArgumentException e) {
					taskcontext.abortExecution(e);
					return null;
				}
			}

			private MavenOperationConfiguration getRepositoryOperationConfiguration() {
				MavenOperationConfiguration config = MavenOperationConfigurationTaskOptionUtils
						.createConfiguration(this.configuration);
				return config;
			}
		};
	}

	private static Object handleLocalizationOutput(TaskContext taskcontext, MavenOperationConfiguration config,
			ArtifactLocalizationTaskOutput localizationoutput) {
		Collection<MavenClassPathEntry> entries = new LinkedHashSet<>();
		final TaskIdentifier sourcelocalizetaskid;
		if (taskcontext.getTaskUtilities()
				.getReportExecutionDependency(IDEConfigurationRequiredExecutionProperty.INSTANCE)) {
			Set<ArtifactCoordinates> sourcelocalizecoordinates = new LinkedHashSet<>();
			for (ArtifactCoordinates coordstr : localizationoutput.getCoordinates()) {
				ArtifactCoordinates sourceacoords = MavenClassPathWorkerTaskFactory
						.createSourceArtifactCoordinates(coordstr);

				sourcelocalizecoordinates.add(sourceacoords);
			}

			TaskFactory<? extends ArtifactLocalizationTaskOutput> dltaskfactory = ArtifactLocalizationUtils
					.createLocalizeArtifactsTaskFactory(config, sourcelocalizecoordinates);
			sourcelocalizetaskid = ArtifactLocalizationUtils.createLocalizeArtifactsTaskIdentifier(config,
					sourcelocalizecoordinates);
			taskcontext.startTask(sourcelocalizetaskid, dltaskfactory, null);
		} else {
			sourcelocalizetaskid = null;
		}
		StructuredListTaskResult locresultslist = localizationoutput.getLocalizationResults();
		Iterator<? extends StructuredTaskResult> it = locresultslist.resultIterator();
		while (it.hasNext()) {
			ArtifactLocalizationWorkerTaskOutput locres = (ArtifactLocalizationWorkerTaskOutput) it.next()
					.toResult(taskcontext);
			MavenClassPathEntry cpentry = new MavenClassPathEntry(LocalFileLocation.create(locres.getLocalPath()),
					locres.getContentDescriptor());
			if (sourcelocalizetaskid != null) {
				ArtifactCoordinates sourceacoords = MavenClassPathWorkerTaskFactory
						.createSourceArtifactCoordinates(locres.getCoordinates());
				cpentry.setSourceAttachment(
						new SourceAttachmentRetrievingStructuredTaskResult(sourcelocalizetaskid, sourceacoords));
			}
			entries.add(cpentry);
		}

		MavenClassPathReference result = new MavenClassPathReference(entries);
		taskcontext.reportSelfTaskOutputChangeDetector(new EqualityTaskOutputChangeDetector(result));
		return result;
	}

	private static Object handleDownloadOutput(TaskContext taskcontext, MavenOperationConfiguration config,
			ArtifactDownloadTaskOutput downloadoutput) {
		Collection<MavenClassPathEntry> entries = new LinkedHashSet<>();
		final TaskIdentifier sourcedltaskid;
		if (taskcontext.getTaskUtilities()
				.getReportExecutionDependency(IDEConfigurationRequiredExecutionProperty.INSTANCE)) {
			Set<ArtifactCoordinates> sourcedlcoordinates = new LinkedHashSet<>();
			for (ArtifactCoordinates coordstr : downloadoutput.getCoordinates()) {
				ArtifactCoordinates sourceacoords = MavenClassPathWorkerTaskFactory
						.createSourceArtifactCoordinates(coordstr);

				sourcedlcoordinates.add(sourceacoords);
			}
			TaskFactory<? extends ArtifactDownloadTaskOutput> dltaskfactory = ArtifactDownloadUtils
					.createDownloadArtifactsTaskFactory(config, sourcedlcoordinates);
			sourcedltaskid = ArtifactDownloadUtils.createDownloadArtifactsTaskIdentifier(config, sourcedlcoordinates);
			taskcontext.startTask(sourcedltaskid, dltaskfactory, null);
		} else {
			sourcedltaskid = null;
		}
		StructuredListTaskResult dlresultslist = downloadoutput.getDownloadResults();
		Iterator<? extends StructuredTaskResult> it = dlresultslist.resultIterator();
		while (it.hasNext()) {
			ArtifactDownloadWorkerTaskOutput dlres = (ArtifactDownloadWorkerTaskOutput) it.next().toResult(taskcontext);
			MavenClassPathEntry cpentry = new MavenClassPathEntry(ExecutionFileLocation.create(dlres.getPath()),
					dlres.getContentDescriptor());
			if (sourcedltaskid != null) {
				ArtifactCoordinates sourceacoords = MavenClassPathWorkerTaskFactory
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

	private static Object handleArtifactCoordinates(TaskContext taskcontext, MavenOperationConfiguration config,
			Set<ArtifactCoordinates> coordinates) {
		MavenClassPathWorkerTaskFactory workertask = new MavenClassPathWorkerTaskFactory(config, coordinates);
		taskcontext.startTask(workertask, workertask, null);

		StructuredTaskResult result = new SimpleStructuredObjectTaskResult(workertask);
		taskcontext.reportSelfTaskOutputChangeDetector(new EqualityTaskOutputChangeDetector(result));
		return result;
	}
}
