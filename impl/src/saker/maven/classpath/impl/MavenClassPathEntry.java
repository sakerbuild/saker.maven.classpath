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
import java.util.Set;

import saker.build.task.utils.StructuredTaskResult;
import saker.java.compiler.api.classpath.ClassPathEntry;
import saker.java.compiler.api.classpath.ClassPathReference;
import saker.java.compiler.api.classpath.JavaSourceDirectory;
import saker.std.api.file.location.FileLocation;

public class MavenClassPathEntry implements ClassPathEntry, Externalizable {
	private static final long serialVersionUID = 1L;

	private FileLocation fileLocation;
	private Object implementationVersionKey;

	private StructuredTaskResult sourceAttachment;
	private StructuredTaskResult documentationAttachment;

	/**
	 * For {@link Externalizable}.
	 */
	public MavenClassPathEntry() {
	}

	public MavenClassPathEntry(FileLocation fileLocation, Object implementationVersionKey,
			StructuredTaskResult sourceAttachment, StructuredTaskResult documentationAttachment) {
		this.fileLocation = fileLocation;
		this.implementationVersionKey = implementationVersionKey;
		this.sourceAttachment = sourceAttachment;
		this.documentationAttachment = documentationAttachment;
	}

	public MavenClassPathEntry(FileLocation fileLocation, Object implementationVersionKey) {
		this.fileLocation = fileLocation;
		this.implementationVersionKey = implementationVersionKey;
	}

	public void setDocumentationAttachment(StructuredTaskResult documentationAttachment) {
		this.documentationAttachment = documentationAttachment;
	}

	public void setSourceAttachment(StructuredTaskResult sourceAttachment) {
		this.sourceAttachment = sourceAttachment;
	}

	public void setFileLocation(FileLocation fileLocation) {
		this.fileLocation = fileLocation;
	}

	public void setImplementationVersionKey(Object implementationVersionKey) {
		this.implementationVersionKey = implementationVersionKey;
	}

	@Override
	public FileLocation getFileLocation() {
		return fileLocation;
	}

	@Override
	public Collection<? extends ClassPathReference> getAdditionalClassPathReferences() {
		return null;
	}

	@Override
	public Set<? extends JavaSourceDirectory> getSourceDirectories() {
		return null;
	}

	@Override
	public Object getAbiVersionKey() {
		return null;
	}

	@Override
	public Object getImplementationVersionKey() {
		return implementationVersionKey;
	}

	@Override
	public StructuredTaskResult getSourceAttachment() {
		return sourceAttachment;
	}

	@Override
	public StructuredTaskResult getDocumentationAttachment() {
		return documentationAttachment;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(fileLocation);
		out.writeObject(implementationVersionKey);
		out.writeObject(sourceAttachment);
		out.writeObject(documentationAttachment);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		fileLocation = (FileLocation) in.readObject();
		implementationVersionKey = in.readObject();
		sourceAttachment = (StructuredTaskResult) in.readObject();
		documentationAttachment = (StructuredTaskResult) in.readObject();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((documentationAttachment == null) ? 0 : documentationAttachment.hashCode());
		result = prime * result + ((implementationVersionKey == null) ? 0 : implementationVersionKey.hashCode());
		result = prime * result + ((fileLocation == null) ? 0 : fileLocation.hashCode());
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
		MavenClassPathEntry other = (MavenClassPathEntry) obj;
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
		if (fileLocation == null) {
			if (other.fileLocation != null)
				return false;
		} else if (!fileLocation.equals(other.fileLocation))
			return false;
		if (sourceAttachment == null) {
			if (other.sourceAttachment != null)
				return false;
		} else if (!sourceAttachment.equals(other.sourceAttachment))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + fileLocation + "]";
	}
}
