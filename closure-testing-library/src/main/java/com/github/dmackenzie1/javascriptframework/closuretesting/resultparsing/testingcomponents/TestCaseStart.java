package com.github.dmackenzie1.javascriptframework.closuretesting.resultparsing.testingcomponents;

import java.util.Calendar;

/**
 * The start timing div.
 * 
 * meant to parse the following:
 * 
 * 14:42:27.465 Done
 *
 */
public class TestCaseStart implements IParsedDivObject {
	/**
	 * Constructor.
	 * @param startTimeIn the time of start
	 */
	public TestCaseStart(final Calendar startTimeIn) {
		startTime = startTimeIn;
	}

	/**
	 * @return the Start Time
	 */
	public final Calendar getStartTime() {
		return startTime;
	}

	/**
	 * The time when the test case was started.
	 */
	private final Calendar startTime;

}
