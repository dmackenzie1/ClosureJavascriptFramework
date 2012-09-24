package com.github.dmackenzie1.javascriptframework.closurecompiler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.github.dmackenzie1.javascriptframework.mavenutils.logging.Log4jOutputStream;
import com.github.dmackenzie1.javascriptframework.mavenutils.logging.MojoLogAppender;
import com.github.dmackenzie1.javascriptframework.mavenutils.mavenobjects.JsarRelativeLocations;
import com.github.dmackenzie1.javascriptframework.mavenutils.pathing.FileListBuilder;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import com.google.javascript.jscomp.CommandLineRunner;
import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.JSSourceFile;
import com.google.javascript.jscomp.Result;
import com.google.javascript.jscomp.SourceFile;
import com.google.javascript.jscomp.WarningLevel;

/**
 * 
 * @goal js-closure-compile
 * @phase compile
 */
public class JsClosureCompileMojo extends AbstractMojo {
	/**
	 * The Logger.
	 */
	private static final Logger LOGGER = Logger
			.getLogger(JsClosureCompileMojo.class);

	/**
	 * A simple util to convert a collection of files to a list of closure
	 * JSSourceFiles.
	 * 
	 * @param jsFiles
	 *            the collection of files to convert
	 * @return the list of google formatted objects
	 */
	private static List<SourceFile> convertToSourceFiles(
			final List<File> jsFiles) {
		List<SourceFile> jsSourceFiles = new ArrayList<SourceFile>();
		for (File f : jsFiles) {
			jsSourceFiles.add(JSSourceFile.fromFile(f));
		}
		return jsSourceFiles;
	}

	/**
	 * Create the dependencies JS file.
	 * 
	 * @param baseLocation
	 *            the location of base.js
	 * @param src
	 *            the location of the source files
	 * @param interns
	 *            the internal dependencies
	 * @param depsFile
	 *            the location of the deps file
	 * @param requiresFile
	 *            the location of the requires file
	 * @return the list of dependencies, in dependency order
	 * @throws MojoExecutionException
	 *             if the dependency generator is not able to make a file
	 * @throws IOException
	 *             if there is a problem reading or writing to any of the files
	 */
	private static List<File> createDepsAndRequiresJS(final File baseLocation,
			final Collection<File> src, final Collection<File> interns,
			final File depsFile, final File requiresFile)
			throws MojoExecutionException, IOException {

		// TODO when they fix the visibility rules in the DepsGenerator, replace
		// it with Google's version
		LOGGER.debug("base location: " + baseLocation);
		LOGGER.debug("src files: " + src);
		LOGGER.debug("intern files: " + interns);
		LOGGER.debug("deps file location: " + depsFile);

		return CalcDeps.executeCalcDeps(baseLocation, src, interns, depsFile,
				requiresFile);
	}

	/**
	 * Get the location of base.js.
	 * 
	 * @param closureLibraryLocation
	 *            the location of the google library
	 * @return the base.js file reference
	 * @throws MojoExecutionException
	 *             If it couldn't find base.js
	 */
	private static File getBaseLocation(final File closureLibraryLocation)
			throws MojoExecutionException {
		File baseLocation = new File(closureLibraryLocation.getAbsoluteFile()
				+ File.separator + "closure" + File.separator + "goog"
				+ File.separator + "base.js");
		if (!baseLocation.exists()) {
			throw new MojoExecutionException(
					"Could not locate \"base.js\" at location \""
							+ baseLocation.getParentFile().getAbsolutePath()
							+ "\"");
		}
		return baseLocation;
	}

	/**
	 * List the errors that google is providing from the compiler output.
	 * 
	 * @param result
	 *            the results from the compiler
	 */
	private static void listErrors(final Result result) {
		for (JSError warning : result.warnings) {
			LOGGER.warn("[Goog.WARN]: " + warning.toString());
		}

		for (JSError error : result.errors) {
			LOGGER.error("[Goog.ERROR]: " + error.toString());
		}
	}

	/**
	 * List the javascript files in a directory.
	 * 
	 * @param directory
	 *            the directory to search
	 * @return the set of files with the ".js" extension
	 */
	private static List<File> listFiles(final File directory) {
		return FileListBuilder.buildFilteredList(directory, "js");
	}

	/**
	 * The location of the closure library. By default, this is expected in the
	 * ${project.build.directory}${file.separator}javascriptFramework${file.
	 * separator}closure-library${file.separator}closure${file.separator}goog
	 * directory, as this is where the jsdependency plugin will put it by
	 * default. If you would like to override this and use your own location,
	 * this can be done by changing this path.
	 * 
	 * @parameter default-value=
	 *            "${project.build.directory}${file.separator}javascriptFramework${file.separator}closure-library"
	 * @required
	 */
	private File closureLibraryLocation;

	/**
	 * The file produced after running the dependencies and files through the
	 * compiler.
	 * 
	 * @parameter default-value="${project.build.finalName}-min.js"
	 * @required
	 */
	private String compiledFilename;

	/**
	 * Specifies the compiler level to use.
	 * 
	 * Possible values are:
	 * <ul>
	 * <li>WHITESPACE_ONLY
	 * <li>SIMPLE_OPTIMIZATIONS
	 * <li>ADVANCED_OPTIMIZATIONS
	 * </ul>
	 * <br/>
	 * 
	 * Please see the <a href=
	 * "http://code.google.com/closure/compiler/docs/compilation_levels.html"
	 * >Google Compiler levels page</a> for more details.
	 * 
	 * @parameter default-value="ADVANCED_OPTIMIZATIONS"
	 * @required
	 */
	private String compileLevel;

	/**
	 * Specifies how strict the compiler is at following the rules. The compiler
	 * is set to parameters matching SIMPLE by default, however, if you don't
	 * use WARNING or better, this isn't very useful. STRICT is set by default
	 * for this mojo.
	 * 
	 * Possible values are:
	 * <ul>
	 * <li>SIMPLE
	 * <li>WARNING
	 * <li>STRICT
	 * </ul>
	 * <br/>
	 * 
	 * @parameter default-value="STRICT"
	 */
	private String errorLevel;

	/**
	 * The file produced after running the dependencies and files through the
	 * compiler.
	 * 
	 * @parameter default-value=
	 *            "${project.build.directory}${file.separator}javascriptFramework"
	 * @required
	 */
	private File frameworkTargetDirectory;

	/**
	 * The file produced after running the dependencies and files through the
	 * compiler.
	 * 
	 * @parameter default-value="${project.build.finalName}-assert.js"
	 * @required
	 */
	private String generatedAssertJS;

	/**
	 * The file produced that allows inclusion of assert into non-closure based
	 * systems.
	 * 
	 * @parameter default-value="${project.build.finalName}-assert-requires.js"
	 * @required
	 */
	private String generatedAssertRequiresJS;

	/**
	 * The file produced after running the dependencies and files through the
	 * compiler.
	 * 
	 * @parameter default-value="${project.build.finalName}-debug.js"
	 * @required
	 */
	private String generatedDebugJS;

	/**
	 * The file produced that allows inclusion of debug into non-closure based
	 * systems.
	 * 
	 * @parameter default-value="${project.build.finalName}-debug-requires.js"
	 * @required
	 */
	private String generatedDebugRequiresJS;

	/**
	 * @parameter default-value="true"
	 */
	private boolean generateExports;

	/**
	 * What to include based off of maven includes. If you are creating an API
	 * with an empty src folder, or you expect to use all of the maven
	 * dependencies as source, set this to ALL.
	 * 
	 * Possible values are:
	 * <ul>
	 * <li>ALL
	 * <li>WHEN_IN_SRCS
	 * </ul>
	 * <br/>
	 * 
	 * @parameter default-value="WHEN_IN_SRCS"
	 */
	private String inclusionStrategy;

	/**
	 * The default directory to extract files to. This likely shouldn't be
	 * changed unless there is a conflict with another plugin.
	 * 
	 * @parameter default-value=
	 *            "${basedir}${file.separator}src${file.separator}test${file.separator}javascript"
	 * @required
	 */
	private File testSourceDirectory;

	/**
	 * Will wrap the code in whatever you put in here. It uses '%output%' to
	 * define what your code is. An example of this would be
	 * "(function() {%output% window['my']['namespace'] = my.namespace;})();",
	 * which would wrap the entire code in an anonymous function.
	 * 
	 * @parameter default-value=""
	 */
	private String outputWrapper = "";

	/**
	 * The string to match the code fragment in the outputWrapper parameter.
	 */
	private static final String OUTPUT_WRAPPER_MARKER = "%output%";

	/**
	 * Extract external dependency libraries to the location specified in the
	 * settings.
	 * 
	 * @return the list of the files that are extracted
	 * @throws IOException
	 *             if unable to read the default externs
	 */
	private List<SourceFile> calculateExternFiles() throws IOException {
		List<File> externSourceFiles = listFiles(JsarRelativeLocations
				.getExternsLocation(frameworkTargetDirectory));
		List<SourceFile> externalSourceFiles = convertToSourceFiles(externSourceFiles);
		externalSourceFiles.addAll(CommandLineRunner.getDefaultExterns());
		LOGGER.debug("number of external files:" + externalSourceFiles.size());
		return externalSourceFiles;
	}

	/**
	 * Extract internal dependency libraries, source to the location specified
	 * in the settings. Then create the deps to be loaded first.
	 * 
	 * @param internsLocation
	 *            the location of the interns files
	 * @param source
	 *            the collection of source files
	 * @return the list of the files that are extracted (plus the generated deps
	 *         file)
	 * @throws MojoExecutionException
	 *             if there is a problem generating the dependency file
	 * @throws IOException
	 *             if there is a problem reading or extracting the files
	 */
	private Collection<File> calculateInternalFiles(final File internsLocation,
			final Collection<File> source) throws MojoExecutionException,
			IOException {
		List<File> internalSourceFiles = listFiles(internsLocation);
		LOGGER.debug("number of internal dependency files:"
				+ internalSourceFiles.size());

		List<File> closureLibFiles = listFiles(closureLibraryLocation);
		LOGGER.debug("number of google lib files:" + closureLibFiles.size());

		HashSet<File> combinedInternal = new HashSet<File>();

		combinedInternal.addAll(source);
		combinedInternal.addAll(internalSourceFiles);
		combinedInternal.addAll(closureLibFiles);

		return combinedInternal;
	}

	/**
	 * Calculates the Source file collection.
	 * 
	 * @param sourceDir
	 *            the source directory to scan
	 * @param internsLocation
	 *            the internal dependency
	 * @return the set of files calculated
	 */
	private Set<File> calculateSourceFiles(final File sourceDir,
			final File internsLocation) {
		InclusionStrategy strategy = InclusionStrategy
				.getByType(inclusionStrategy);
		if (strategy == null) {
			strategy = InclusionStrategy.WHEN_IN_SRCS;
		}
		LOGGER.info("Calculating source files using Inclusion strategy: "
				+ strategy);
		Set<File> listSourceFiles = new HashSet<File>();
		if (strategy.equals(InclusionStrategy.WHEN_IN_SRCS)) {
			listSourceFiles.addAll(listFiles(sourceDir));
		} else {
			listSourceFiles.addAll(listFiles(sourceDir));
			listSourceFiles.addAll(listFiles(internsLocation));
		}
		LOGGER.debug("number of source files:" + listSourceFiles.size());
		return listSourceFiles;
	}

	/**
	 * Run the compiler on the calculated dependencies, input files, and
	 * external files.
	 * 
	 * @param allSources
	 *            the source files to compile
	 * @param externs
	 *            the external dependency javascript files
	 * @return true if the compile works, false otherwise
	 * @throws MojoExecutionException
	 *             if the options are set incorrectly for the compiler
	 * @throws MojoFailureException
	 *             if there is a problem executing the dependency creation or
	 *             the compiler
	 * @throws IOException
	 *             if there is a problem reading or writing to the files
	 */
	private boolean compile(final List<SourceFile> allSources,
			final List<SourceFile> externs)
			throws MojoExecutionException, MojoFailureException, IOException {
		CompilationLevel compilationLevel = null;
		try {
			compilationLevel = CompilationLevel.valueOf(compileLevel);
			LOGGER.info("Compiler set to optimization level \"" + compileLevel
					+ "\".");
		} catch (IllegalArgumentException e) {
			LOGGER.error("Compilation level invalid.  Aborting.");
			throw new MojoExecutionException(
					"Compilation level invalid.  Aborting.");
		}

		CompilerOptions compilerOptions = new CompilerOptions();
		if (ErrorLevel.getCompileLevelByName(errorLevel).equals(
				ErrorLevel.WARNING)) {
			WarningLevel wLevel = WarningLevel.VERBOSE;
			wLevel.setOptionsForWarningLevel(compilerOptions);
		} else if (ErrorLevel.getCompileLevelByName(errorLevel).equals(
				ErrorLevel.STRICT)) {
			StrictLevel sLevel = StrictLevel.VERBOSE;
			sLevel.setOptionsForWarningLevel(compilerOptions);
		}
		compilationLevel.setOptionsForCompilationLevel(compilerOptions);
		compilerOptions.setGenerateExports(generateExports);

		PrintStream ps = new PrintStream(new Log4jOutputStream(LOGGER,
				Level.DEBUG), true);
		Compiler compiler = new Compiler(ps);

		for (SourceFile jsf : allSources) {
			LOGGER.debug("source files: " + jsf.getOriginalPath());
		}

		Result result = null;
		try {
			LOGGER.debug("externJSSourceFiles: " + externs);
			LOGGER.debug("allSources: " + allSources);
			result = compiler.compile(
					externs,
					allSources,
					compilerOptions);
		} catch (Exception e) {
			LOGGER.error("There was a problem with the compile.  Please review input.");
			e.printStackTrace();
			throw new MojoExecutionException(e.getMessage(), e);
		}

		listErrors(result);

		if (!result.success) {
			return false;
		}

		File compiledFile = new File(
				JsarRelativeLocations
						.getCompileLocation(frameworkTargetDirectory),
				compiledFilename);
		Files.createParentDirs(compiledFile);
		Files.touch(compiledFile);
		JsClosureCompileMojo.writeOutput(compiledFile, compiler, outputWrapper,
				OUTPUT_WRAPPER_MARKER);

		return true;
	}

	@Override
	public final void execute() throws MojoExecutionException,
			MojoFailureException {
		MojoLogAppender.beginLogging(this);
		try {
			LOGGER.info("Compiling source files and internal dependencies to location \""
					+ JsarRelativeLocations.getCompileLocation(
							frameworkTargetDirectory).getAbsolutePath() + "\".");
			// gather externs for both asserts and debug
			List<SourceFile> externs = calculateExternFiles();

			// get base location for closure library
			File baseLocation = getBaseLocation(closureLibraryLocation);

			// create assert file
			Collection<File> assertSourceFiles = calculateSourceFiles(
					JsarRelativeLocations
							.getAssertionSourceLocation(frameworkTargetDirectory),
					JsarRelativeLocations
							.getInternsAssertLocation(frameworkTargetDirectory));
			File assertFile = getGeneratedAssertJS();
			File assertRequiresFile = getGeneratedAssertRequiresJS();
			Collection<File> assertInternFiles = calculateInternalFiles(
					JsarRelativeLocations
							.getInternsAssertLocation(frameworkTargetDirectory),
					assertSourceFiles);
			createDepsAndRequiresJS(baseLocation, assertSourceFiles,
					assertInternFiles, assertFile, assertRequiresFile);

			// create debug file
			File debugFile = getGeneratedDebugJS();
			File debugRequiresFile = getGeneratedDebugRequiresJS();
			Collection<File> sourceFiles = calculateSourceFiles(
					JsarRelativeLocations
							.getDebugSourceLocation(frameworkTargetDirectory),
					JsarRelativeLocations
							.getInternsDebugLocation(frameworkTargetDirectory));
			Collection<File> debugInternFiles = calculateInternalFiles(
					JsarRelativeLocations
							.getInternsDebugLocation(frameworkTargetDirectory),
					sourceFiles);
			List<File> debugDepsFiles = createDepsAndRequiresJS(baseLocation,
					sourceFiles, debugInternFiles, debugFile, debugRequiresFile);

			// create testing file
			File testDepsFile = getGeneratedTestJS();
			Collection<File> srcAndTest = new HashSet<File>();
			srcAndTest.addAll(assertSourceFiles);
			srcAndTest.addAll(FileListBuilder.buildFilteredList(
					testSourceDirectory, "js"));
			createDepsAndRequiresJS(baseLocation, srcAndTest,
					assertInternFiles, testDepsFile, null);

			// create file collection for compilation
			List<File> debugFiles = new ArrayList<File>();
			debugFiles.add(getBaseLocation(closureLibraryLocation));
			debugFiles.add(debugFile);
			debugFiles.addAll(debugDepsFiles);

			// compile debug into compiled dir
			boolean result = compile(convertToSourceFiles(debugFiles),
					externs);

			if (!result) {
				String message = "Google Closure Compilation failure.  Please review errors to continue.";
				LOGGER.error(message);
				throw new MojoFailureException(message);
			}
		} catch (Exception e) {
			e.printStackTrace();
			e.printStackTrace(new PrintStream(new Log4jOutputStream(LOGGER,
					Level.DEBUG), true));
			throw new MojoExecutionException(
					"Unable to closure compile files: " + e.getMessage());
		} finally {
			MojoLogAppender.endLogging();
		}
	}

	/**
	 * Will write the output file, including the wrapper around the code, if any
	 * exist.
	 * 
	 * @param outFile
	 *            The file to write to
	 * @param compiler
	 *            The google compiler
	 * @param wrapper
	 *            the string to wrap around the code (using the codePlaceholder)
	 * @param codePlaceholder
	 *            the identifier for the code
	 * @throws IOException
	 *             when the file cannot be written to.
	 */
	static void writeOutput(final File outFile, final Compiler compiler,
			final String wrapper, final String codePlaceholder)
			throws IOException {
		FileWriter out = new FileWriter(outFile);
		String code = compiler.toSource();
		boolean threw = true;
		try {
			int pos = wrapper.indexOf(codePlaceholder);
			LOGGER.debug("wrapper = " + wrapper);
			if (pos != -1) {
				String prefix = "";

				if (pos > 0) {
					prefix = wrapper.substring(0, pos);
					LOGGER.debug("prefix" + prefix);
					out.append(prefix);
				}

				out.append(code);

				int suffixStart = pos + codePlaceholder.length();
				if (suffixStart != wrapper.length()) {
					LOGGER.debug("suffix" + wrapper.substring(suffixStart));
					// Something after placeholder?
					out.append(wrapper.substring(suffixStart));
				}
				// Make sure we always end output with a line feed.
				out.append('\n');

				// If we have a source map, adjust its offsets to match
				// the code WITHIN the wrapper.
				if (compiler != null && compiler.getSourceMap() != null) {
					compiler.getSourceMap().setWrapperPrefix(prefix);
				}

			} else {
				out.append(code);
				out.append('\n');
			}
			threw = false;
		} finally {
			Closeables.close(out, threw);
		}
	}

	/**
	 * @return the generated assert javascript file
	 */
	private File getGeneratedAssertJS() {
		return new File(
				JsarRelativeLocations
						.getAssertDepsLocation(frameworkTargetDirectory),
				generatedAssertJS);
	}

	/**
	 * @return the generated debug javascript file
	 */
	private File getGeneratedDebugJS() {
		return new File(
				JsarRelativeLocations
						.getDebugDepsLocation(frameworkTargetDirectory),
				generatedDebugJS);
	}

	/**
	 * @return the generated test javascript file
	 */
	private File getGeneratedTestJS() {
		return new File(
				JsarRelativeLocations
						.getTestDepsLocation(frameworkTargetDirectory),
				generatedAssertJS);
	}

	/**
	 * @return the generated assert requires javascript file
	 */
	private File getGeneratedAssertRequiresJS() {
		return new File(
				JsarRelativeLocations
						.getAssertRequiresLocation(frameworkTargetDirectory),
				generatedAssertRequiresJS);
	}

	/**
	 * @return the generated debug requires javascript file
	 */
	private File getGeneratedDebugRequiresJS() {
		return new File(
				JsarRelativeLocations
						.getDebugRequiresLocation(frameworkTargetDirectory),
				generatedDebugRequiresJS);
	}
}