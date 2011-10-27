package com.github.jlgrock.javascriptframework.closuretesting.resultparsing.generators;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Will generate a set of test case files. This expects that all source files
 * have been verified as JavaScript prior to usage in this Suite.
 * 
 */
public class SuiteGenerator {
	/**
	 * The set of the source files to convert.
	 */
	private final Set<File> sourceFiles;

	/**
	 * The location of the closure library base.js.
	 */
	private final File closureLibraryBaseLocation;

	/**
	 * The location of the generated dependency file.
	 */
	private final File dependencyLocation;

	/**
	 * Constructor.
	 * 
	 * @param sourceFilesIn
	 *            the source files to process as part of the suite. Assumed all
	 *            are javascript
	 * @param closureLibraryBaseLocationIn
	 *            the location of the closure library (which contains base.js)
	 * @param dependencyLocationIn
	 *            the location of the generated dependency file
	 */
	public SuiteGenerator(final Set<File> sourceFilesIn,
			final File closureLibraryBaseLocationIn, final File dependencyLocationIn) {
		this.sourceFiles = sourceFilesIn;
		this.closureLibraryBaseLocation = closureLibraryBaseLocationIn;
		this.dependencyLocation = dependencyLocationIn;
	}

	/**
	 * Generates the test case objects from the Source Files provided.
	 * 
	 * @return the test case objects
	 */
	private Set<TestCaseGenerator> generateTestCases() {
		Set<TestCaseGenerator> testCaseGenerators = new HashSet<TestCaseGenerator>();
		for (File sourceFile : sourceFiles) {
			testCaseGenerators.add(new TestCaseGenerator(
					closureLibraryBaseLocation, dependencyLocation, sourceFile));
		}
		return testCaseGenerators;
	}

	/**
	 * Creates the set of test files.
	 * 
	 * @param outputDirectory
	 *            the output directory to generate the files to
	 * @return the set of files created
	 * @throws IOException
	 *             if unable to create one or more test cases
	 */
	public final Set<File> generateTestFiles(final File outputDirectory)
			throws IOException {
		Set<File> outputFiles = new HashSet<File>();
		Set<TestCaseGenerator> generatedTestCases = generateTestCases();
		for (TestCaseGenerator testCaseGenerator : generatedTestCases) {
			File testCase = testCaseGenerator.createTestCase(outputDirectory);
			outputFiles.add(testCase);
		}
		return outputFiles;
	}

}
