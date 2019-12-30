package saker.maven.classpath.main;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import saker.build.runtime.execution.SakerLog;
import saker.build.task.TaskResultResolver;
import saker.build.task.identifier.TaskIdentifier;
import saker.build.task.utils.StructuredTaskResult;
import saker.maven.support.api.ArtifactCoordinates;
import saker.maven.support.api.download.ArtifactDownloadTaskOutput;
import saker.maven.support.api.download.ArtifactDownloadWorkerTaskOutput;
import saker.maven.support.api.localize.ArtifactLocalizationTaskOutput;
import saker.maven.support.api.localize.ArtifactLocalizationWorkerTaskOutput;
import saker.std.api.file.location.ExecutionFileLocation;
import saker.std.api.file.location.LocalFileLocation;

class SourceAttachmentRetrievingStructuredTaskResult implements StructuredTaskResult, Externalizable {
	private static final long serialVersionUID = 1L;

	private TaskIdentifier downloadOrLocalizeTaskId;
	private ArtifactCoordinates coordinates;

	/**
	 * For {@link Externalizable}.
	 */
	public SourceAttachmentRetrievingStructuredTaskResult() {
	}

	public SourceAttachmentRetrievingStructuredTaskResult(TaskIdentifier downloadOrLocalizeTaskId, ArtifactCoordinates coordinates) {
		this.downloadOrLocalizeTaskId = downloadOrLocalizeTaskId;
		this.coordinates = coordinates;
	}

	@Override
	public Object toResult(TaskResultResolver results) {
		Object actualres = StructuredTaskResult.getActualTaskResult(downloadOrLocalizeTaskId, results);
		if (actualres instanceof ArtifactDownloadTaskOutput) {
			try {
				ArtifactDownloadTaskOutput dloutput = (ArtifactDownloadTaskOutput) actualres;
				StructuredTaskResult coorddlres = dloutput.getDownloadResult(coordinates);
				if (coorddlres == null) {
					return null;
				}
				Object dlresult = coorddlres.toResult(results);
				if (!(dlresult instanceof ArtifactDownloadWorkerTaskOutput)) {
					return null;
				}
				ArtifactDownloadWorkerTaskOutput dlworkerout = (ArtifactDownloadWorkerTaskOutput) dlresult;
				return ExecutionFileLocation.create(dlworkerout.getPath());
			} catch (Exception e) {
				SakerLog.log().verbose()
						.println("Failed to retrieve source attachment: " + coordinates + " (" + e + ")");
			}
		}
		if (actualres instanceof ArtifactLocalizationTaskOutput) {
			try {
				ArtifactLocalizationTaskOutput dloutput = (ArtifactLocalizationTaskOutput) actualres;
				StructuredTaskResult coorddlres = dloutput.getLocalizationResult(coordinates);
				if (coorddlres == null) {
					return null;
				}
				Object dlresult = coorddlres.toResult(results);
				if (!(dlresult instanceof ArtifactLocalizationWorkerTaskOutput)) {
					return null;
				}
				ArtifactLocalizationWorkerTaskOutput dlworkerout = (ArtifactLocalizationWorkerTaskOutput) dlresult;
				return LocalFileLocation.create(dlworkerout.getLocalPath());
			} catch (Exception e) {
				SakerLog.log().verbose()
						.println("Failed to retrieve source attachment: " + coordinates + " (" + e + ")");
			}
		}
		return null;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(downloadOrLocalizeTaskId);
		out.writeObject(coordinates);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		downloadOrLocalizeTaskId = (TaskIdentifier) in.readObject();
		coordinates = (ArtifactCoordinates) in.readObject();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((coordinates == null) ? 0 : coordinates.hashCode());
		result = prime * result + ((downloadOrLocalizeTaskId == null) ? 0 : downloadOrLocalizeTaskId.hashCode());
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
		SourceAttachmentRetrievingStructuredTaskResult other = (SourceAttachmentRetrievingStructuredTaskResult) obj;
		if (coordinates == null) {
			if (other.coordinates != null)
				return false;
		} else if (!coordinates.equals(other.coordinates))
			return false;
		if (downloadOrLocalizeTaskId == null) {
			if (other.downloadOrLocalizeTaskId != null)
				return false;
		} else if (!downloadOrLocalizeTaskId.equals(other.downloadOrLocalizeTaskId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + coordinates + "]";
	}

}