package io.github.grigoryrylov.android.test;

import org.gradle.fork.api.tasks.testing.TestResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by grishberg on 08.04.18.
 */
public class TestResultExt extends TestResultModel implements Comparable<TestResultExt> {
    private final long duration;
    private final String device;
    private final String project;
    private final String flavor;
    final ClassTestResultsExt classResults;
    final List<TestResultExt.TestFailure> failures = new ArrayList<>();
    final List<TestResultExt> ignoredTests = new ArrayList<>();
    final String name;
    private boolean ignored;

    public TestResultExt(String name, long duration, String device, String project, String flavor,
                         ClassTestResultsExt classResults) {
        this.name = name;
        this.duration = duration;
        this.device = device;
        this.project = project;
        this.flavor = flavor;
        this.classResults = classResults;
    }

    public Object getId() {
        return name;
    }

    public String getName() {
        return name;
    }

    public String getDevice() {
        return device;
    }

    public String getProject() {
        return project;
    }

    public String getFlavor() {
        return flavor;
    }

    @Override
    public String getTitle() {
        return String.format("Test %s", name);
    }

    @Override
    public TestResult.ResultType getResultType() {
        if (ignored) {
            return TestResult.ResultType.SKIPPED;
        }
        return failures.isEmpty() ? TestResult.ResultType.SUCCESS : TestResult.ResultType.FAILURE;
    }

    @Override
    public long getDuration() {
        return duration;
    }

    @Override
    public String getFormattedDuration() {
        return ignored ? "-" : super.getFormattedDuration();
    }

    public ClassTestResultsExt getClassResults() {
        return classResults;
    }

    public List<TestResultExt.TestFailure> getFailures() {
        return failures;
    }

    public void addFailure(String message, String stackTrace,
                           String deviceName, String projectName, String flavorName,
                           String screenshotPath) {
        classResults.failed(this, deviceName, projectName, flavorName);
        failures.add(new TestResultExt.TestFailure(message, stackTrace, null, screenshotPath));
    }

    public void ignored() {
        ignored = true;
        classResults.addIgnoredTest(this);
        ignoredTests.add(this);
    }

    @Override
    public int compareTo(TestResultExt testResult) {
        int diff = classResults.getName().compareTo(testResult.classResults.getName());
        if (diff != 0) {
            return diff;
        }

        diff = name.compareTo(testResult.name);
        if (diff != 0) {
            return diff;
        }

        diff = device.compareTo(testResult.device);
        if (diff != 0) {
            return diff;
        }

        diff = flavor.compareTo(testResult.flavor);
        if (diff != 0) {
            return diff;
        }

        Integer thisIdentity = System.identityHashCode(this);
        int otherIdentity = System.identityHashCode(testResult);
        return thisIdentity.compareTo(otherIdentity);
    }

    public static class TestFailure {
        private final String message;
        private final String stackTrace;
        private final String exceptionType;
        private final String screenshotPath;

        public TestFailure(String message, String stackTrace, String exceptionType,
                           String screenshotPath) {
            this.message = message;
            this.stackTrace = stackTrace;
            this.exceptionType = exceptionType;
            this.screenshotPath = screenshotPath;
        }

        public String getMessage() {
            return message;
        }

        public String getStackTrace() {
            return stackTrace;
        }

        public String getExceptionType() {
            return exceptionType;
        }

        public String getScreenshotPath() {
            return screenshotPath;
        }
    }
}
