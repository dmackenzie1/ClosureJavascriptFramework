package com.github.dmackenzie1.javascriptframework.jsar;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.github.dmackenzie1.javascriptframework.mavenutils.io.DirectoryIO;
import com.github.dmackenzie1.javascriptframework.mavenutils.mavenobjects.JsarRelativeLocations;

/**
 * A simple extension that copies the necessary files from the "default directory" 
 * to the war staging directory prior to a war packaging.  This is only used in the war lifecycle.
 * 
 * @goal war-move
 * @phase pre-package
 * @threadSafe
 * @requiresDependencyResolution runtime
 */
public class WarMoveMojo extends AbstractMojo {
	/**
	 * The directory to place compiled files into.
	 * 
	 * @parameter default-value=
	 *            "${project.build.directory}${file.separator}javascriptFramework"
	 */
	private File frameworkTargetDirectory;

	/**
	 * The directory (where the webapp is built) that you want to place the debug and assert javascript.
	 * This includes the closure library and all dependencies.
	 * Default value is: ${project.build.directory}/${project.build.finalName}/javascript/debug.
	 * 
	 * @parameter default-value=
	 *            "${project.build.directory}/${project.build.finalName}/javascript/debug"
	 */
	private File debugDirectory;

	/**
	 * The directory (where the webapp is built) that you want to place the deploy javascript.  
	 * This includes the compiled version of the code.
	 * Default value is: ${project.build.directory}/${project.build.finalName}/javascript/compiled.
	 * 
	 * @parameter default-value=
	 *            "${project.build.directory}${file.separator}${project.build.finalName}${file.separator}javascript${file.separator}compiled"
	 */
	private File compileDirectory;

	/**
	 * Whether or not to expand the debug and assertion library. This can be
	 * useful when including files for an api. When doing this, make sure to
	 * include the appropriate internal dependencies and closure compiler.
	 * 
	 * @parameter default-value="true"
	 */
	private boolean includeAssert;

	/**
	 * Whether or not to expand the debug and assertion library. This can be
	 * useful when including files for an api. When doing this, make sure to
	 * include the appropriate internal dependencies and closure compiler.
	 * 
	 * @parameter default-value="true"
	 */
	private boolean includeDebug;

	/**
	 * Whether or not to expand the compressed library files. This can be useful
	 * when including files for an api. When doing this, make sure to include
	 * the appropriate internal dependencies and closure compiler.
	 * 
	 * @parameter default-value="true"
	 */
	private boolean includeCompiled;

	@Override
	public final void execute() throws MojoExecutionException, MojoFailureException {
		try {
			if (includeAssert || includeDebug) {
				if (!debugDirectory.exists()) {
					debugDirectory.mkdirs();
				}
				
				DirectoryIO.copyDirectory(JsarRelativeLocations.getClosureLibraryLocation(frameworkTargetDirectory),
						JsarRelativeLocations.getClosureLibraryLocation(debugDirectory));

				if (JsarRelativeLocations.getInternsLocation(frameworkTargetDirectory).exists()) {
					DirectoryIO.copyDirectory(JsarRelativeLocations.getInternsLocation(frameworkTargetDirectory),
						JsarRelativeLocations.getInternsLocation(debugDirectory));
				}
				if (includeAssert) {
					DirectoryIO.copyDirectory(JsarRelativeLocations.getAssertDepsLocation(frameworkTargetDirectory),
							JsarRelativeLocations.getAssertDepsLocation(debugDirectory));
					DirectoryIO.copyDirectory(JsarRelativeLocations.getAssertionSourceLocation(frameworkTargetDirectory),
							JsarRelativeLocations.getAssertionSourceLocation(debugDirectory));
					DirectoryIO.copyDirectory(JsarRelativeLocations.getAssertRequiresLocation(frameworkTargetDirectory),
							JsarRelativeLocations.getAssertRequiresLocation(debugDirectory));
				}
				if (includeDebug) {
					DirectoryIO.copyDirectory(JsarRelativeLocations.getDebugDepsLocation(frameworkTargetDirectory),
							JsarRelativeLocations.getDebugDepsLocation(debugDirectory));
					DirectoryIO.copyDirectory(JsarRelativeLocations.getDebugSourceLocation(frameworkTargetDirectory),
							JsarRelativeLocations.getDebugSourceLocation(debugDirectory));
					DirectoryIO.copyDirectory(JsarRelativeLocations.getDebugRequiresLocation(frameworkTargetDirectory),
							JsarRelativeLocations.getDebugRequiresLocation(debugDirectory));
				}
			}
			
			if (includeCompiled) {
				DirectoryIO.copyDirectory(JsarRelativeLocations.getCompileLocation(frameworkTargetDirectory),
						compileDirectory);
			}
		} catch (Exception e) {
			throw new MojoExecutionException("Unable to move files to war archive directory: " + e);
		}
	}	
}
