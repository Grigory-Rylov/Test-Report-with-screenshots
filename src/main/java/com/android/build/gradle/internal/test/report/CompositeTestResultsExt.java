package com.android.build.gradle.internal.test.report;

import com.android.builder.core.BuilderConstants;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Created by grishberg on 08.04.18.
 */
public abstract class CompositeTestResultsExt extends TestResultModel {
    private final CompositeTestResultsExt parent;
    private int tests;
    private final Set<TestResultExt> failures = new TreeSet<>();
    private final Set<TestResultExt> ignored = new TreeSet<>();
    private long duration;
    private final Map<String, DeviceTestResultsExt> devices = new TreeMap<>();
    private final Map<String, VariantTestResultsExt> variants = new TreeMap<>();

    protected CompositeTestResultsExt(CompositeTestResultsExt parent) {
        this.parent = parent;
    }

    public String getFilename(ReportType reportType) {
        return getName();
    }

    public abstract String getName();

    public int getTestCount() {
        return tests;
    }

    public int getFailureCount() {
        return failures.size();
    }

    public int getIgnoredCount() {
        return ignored.size();
    }

    @Override
    public long getDuration() {
        return duration;
    }

    @Override
    public String getFormattedDuration() {
        return getTestCount() == 0 ? "-" : super.getFormattedDuration();
    }

    public Set<TestResultExt> getFailures() {
        return failures;
    }

    Map<String, DeviceTestResultsExt> getResultsPerDevices() {
        return devices;
    }

    Map<String, VariantTestResultsExt> getResultsPerVariants() {
        return variants;
    }

    @Override
    public org.gradle.api.tasks.testing.TestResult.ResultType getResultType() {
        return failures.isEmpty() ? org.gradle.api.tasks.testing.TestResult.ResultType.SUCCESS : org.gradle.api.tasks.testing.TestResult.ResultType.FAILURE;
    }

    public String getFormattedSuccessRate() {
        Number successRate = getSuccessRate();
        if (successRate == null) {
            return "-";
        }
        return successRate + "%";
    }

    public Number getSuccessRate() {
        if (getTestCount() == 0) {
            return null;
        }

        BigDecimal tests = BigDecimal.valueOf(getTestCount());
        BigDecimal successful = BigDecimal.valueOf(getTestCount() - getFailureCount());

        return successful.divide(tests, 2,
                BigDecimal.ROUND_DOWN).multiply(BigDecimal.valueOf(100)).intValue();
    }

    protected void failed(TestResultExt failedTest,
                          String deviceName, String projectName, String flavorName) {
        failures.add(failedTest);
        if (parent != null) {
            parent.failed(failedTest, deviceName, projectName, flavorName);
        }

        DeviceTestResultsExt deviceResults = devices.get(deviceName);
        if (deviceResults != null) {
            deviceResults.failed(failedTest, deviceName, projectName, flavorName);
        }

        String key = getVariantKey(projectName, flavorName);
        VariantTestResultsExt variantResults = variants.get(key);
        if (variantResults != null) {
            variantResults.failed(failedTest, deviceName, projectName, flavorName);
        }
    }

    protected TestResultExt addTest(TestResultExt test) {
        tests++;
        duration += test.getDuration();
        return test;
    }

    protected void addIgnoredTest(TestResultExt test) {
        ignored.add(test);
        if (parent != null) {
            parent.addIgnoredTest(test);
        }
        String deviceName = test.getDevice();
        DeviceTestResultsExt deviceResults = devices.get(deviceName);
        if (deviceResults != null) {
            deviceResults.addIgnoredTest(test);
        }

        String key = getVariantKey(test.getProject(), test.getFlavor());
        VariantTestResultsExt variantResults = variants.get(key);
        if (variantResults != null) {
            variantResults.addIgnoredTest(test);
        }
    }

    protected void addDevice(String deviceName, TestResultExt testResult) {
        DeviceTestResultsExt deviceResults = devices.get(deviceName);
        if (deviceResults == null) {
            deviceResults = new DeviceTestResultsExt(deviceName, null);
            devices.put(deviceName, deviceResults);
        }

        deviceResults.addTest(testResult);
    }

    protected void addVariant(String projectName, String flavorName, TestResultExt testResult) {
        String key = getVariantKey(projectName, flavorName);
        VariantTestResultsExt variantResults = variants.get(key);
        if (variantResults == null) {
            variantResults = new VariantTestResultsExt(key, null);
            variants.put(key, variantResults);
        }

        variantResults.addTest(testResult);
    }

    private static String getVariantKey(String projectName, String flavorName) {
        if (BuilderConstants.MAIN.equalsIgnoreCase(flavorName)) {
            return projectName;
        }

        return projectName + ":" + flavorName;
    }
}
