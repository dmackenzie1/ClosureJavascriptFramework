package com.github.dmackenzie1.javascriptframework.mavenutils.mavenobjects;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.zip.ZipInputStream;

import org.apache.log4j.Logger;
import org.apache.maven.artifact.Artifact;

import com.github.dmackenzie1.javascriptframework.mavenutils.io.ZipUtils;

/**
 * This will extract the artifacts specified by the scope and packagingType and
 * will store these files in a directory that matches their artifact coordinates
 * followed by package information.
 * 
 */
public class ArtifactExtractor {
	/**
	 * The Logger.
	 */
	private static final Logger LOGGER = Logger
			.getLogger(ArtifactExtractor.class);

	/**
	 * The artifacts that need to be extracted.
	 */
	private final Set<Artifact> artifacts;

	/**
	 * Constructor.
	 * 
	 * @param artifactsIn
	 *            the artifacts to extract
	 */
	public ArtifactExtractor(final Set<Artifact> artifactsIn) {
		this.artifacts = artifactsIn;
	}

	/**
	 * The action to extract the artifacts that were set at the constructor.
	 * 
	 * @param packagingType
	 *            what type of arhives to extract based on the packaging
	 * @param scope
	 *            what type of arhives to extract based on the scope
	 * @param outputDirectory
	 *            the directory to extract to
	 * @throws IOException
	 *             if there is a problem unzipping or copying files
	 */
	public final void extract(final PackagingType packagingType,
			final ScopeType scope, final File outputDirectory)
			throws IOException {
		extract(null, packagingType, scope, outputDirectory);
	}

	/**
	 * The action to extract the artifacts that were set at the constructor.
	 * 
	 * @param zipEntryName
	 *            the name of the file within the zip to extract
	 * @param packagingType
	 *            what type of arhives to extract based on the packaging
	 * @param scope
	 *            what type of arhives to extract based on the scope
	 * @param outputDirectory
	 *            the directory to extract to
	 * @throws IOException
	 *             if there is a problem unzipping or copying files
	 */
	public final void extract(final String zipEntryName,
			final PackagingType packagingType, final ScopeType scope,
			final File outputDirectory) throws IOException {
		LOGGER.debug("unfiltered artifacts size (" + packagingType + ":"
				+ scope + ") : " + artifacts.size());

		// filter artifacts
		Set<Artifact> filteredArtifacts = filterArtifactList(packagingType,
				scope);
		LOGGER.debug("filtered artifacts size (" + packagingType + "/" + scope
				+ "): " + filteredArtifacts.size());
		// unzip to directory
		if (zipEntryName != null) {
			extractSet(zipEntryName, filteredArtifacts, outputDirectory);
		} else {
			extractSet(filteredArtifacts, outputDirectory);
		}
	}

	/**
	 * Extract a set of artifacts.
	 * 
	 * @param filteredArtifacts
	 *            the artifacts to extract
	 * @param outputDirectory
	 *            the directory to copy to
	 * @throws IOException
	 *             if there is a problem unzipping or copying the files
	 */
	private void extractSet(final Set<? extends Artifact> filteredArtifacts,
			final File outputDirectory) throws IOException {
		extractSet(null, filteredArtifacts, outputDirectory);
	}

	/**
	 * Extract a set of artifacts.
	 * 
	 * @param zipEntryName
	 *            the name of the file within the artifact
	 * @param filteredArtifacts
	 *            the artifacts to extract
	 * @param outputDirectory
	 *            the directory to copy to
	 * @throws IOException
	 *             if there is a problem unzipping or copying the files
	 */
	private void extractSet(final String zipEntryName,
			final Set<? extends Artifact> filteredArtifacts,
			final File outputDirectory) throws IOException {
		for (Artifact artifact : filteredArtifacts) {
			LOGGER.debug("Processing artifact \"" + artifact.getArtifactId() + "\"");
			File file = artifact.getFile();
			InputStream is = new FileInputStream(file);
			ZipInputStream zis = new ZipInputStream(is);
			if (zipEntryName == null) {
				ZipUtils.unzip(zis, outputDirectory);
			} else {
				ZipUtils.unzip(zis, zipEntryName, outputDirectory);
			}
		}
	}

	/**
	 * Filter an Artifact List.
	 * 
	 * @param packagingType
	 *            the packaging type to filter by
	 * @param scopeType
	 *            the scope type to filter by
	 * @return the filtered set of artifacts
	 */
	private Set<Artifact> filterArtifactList(final PackagingType packagingType,
			final ScopeType scopeType) {
		Set<Artifact> returnArtifacts = new LinkedHashSet<Artifact>();
		for (Artifact a : artifacts) {

			ScopeType artifactScopeType = ScopeType.getByName(a.getScope());
			if (artifactScopeType == null) {
				artifactScopeType = ScopeType.COMPILE;
			}
			PackagingType artifactPackagingType = PackagingType.getByName(a
					.getType());
			if (artifactPackagingType == null) {
				artifactPackagingType = PackagingType.JAR;
			}
			if ((scopeType.equals(artifactScopeType) || scopeType
					.equals(ScopeType.ANY))
					&& (packagingType.equals(artifactPackagingType) || packagingType
							.equals(PackagingType.ANY))) {
				returnArtifacts.add(a);
			}
		}
		return returnArtifacts;
	}
}
