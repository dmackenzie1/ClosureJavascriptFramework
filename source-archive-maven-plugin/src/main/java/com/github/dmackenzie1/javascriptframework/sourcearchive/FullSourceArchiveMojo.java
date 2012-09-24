package com.github.dmackenzie1.javascriptframework.sourcearchive;

import org.apache.maven.project.MavenProject;

/**
 * This Mojo will zip everything in the declared source directory into
 * a Source Archive file, which will be stored into the local
 * repository.
 * 
 * @author <a href="mailto:grantjl@umich.edu">Justin Grant</a>
 * @requiresProject
 * @goal full-source-archive
 * @phase package
 * @inheritByDefault false
 * @threadSafe
 */
public class FullSourceArchiveMojo extends AbstractSourceArchiveMojo {

	/**
	 * The resource to assemble with.
	 */
	public static final String DESCRIPTOR_RESOURCE_NAME = "full-src-assembly.xml";
	
    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    @Override
    public final MavenProject getProject() {
        return project;
    }

    @Override
	protected final String getDescriptorResourceName() {
		return DESCRIPTOR_RESOURCE_NAME;
	}
}
