package com.android.build.gradle.internal.test.report;

import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by grishberg on 08.04.18.
 */
public class ClassTestResultsExt extends CompositeTestResultsExt {
    private final String name;
    private final PackageTestResultsExt packageResults;
    private final Set<TestResultExt> results = new TreeSet<>();
    private final StringBuilder standardOutput = new StringBuilder();
    private final StringBuilder standardError = new StringBuilder();

    public ClassTestResultsExt(String name, PackageTestResultsExt packageResults) {
        super(packageResults);
        this.name = name;
        this.packageResults = packageResults;
    }

    @Override
    public String getTitle() {
        return String.format("Class %s", name);
    }

    @Override
    public String getName() {
        return name;
    }

    public String getSimpleName() {
        int pos = name.lastIndexOf(".");
        if (pos != -1) {
            return name.substring(pos + 1);
        }
        return name;
    }

    public PackageTestResultsExt getPackageResults() {
        return packageResults;
    }

    public Map<String, Map<String, TestResultExt>> getTestResultsMap() {
        Map<String, Map<String, TestResultExt>> map = Maps.newHashMap();
        for (TestResultExt result : results) {
            String device = result.getDevice();

            Map<String, TestResultExt> deviceMap = map.get(device);
            if (deviceMap == null) {
                deviceMap = Maps.newHashMap();
                map.put(device, deviceMap);
            }

            deviceMap.put(result.getName(), result);
        }

        return map;
    }

    public CharSequence getStandardError() {
        return standardError;
    }

    public CharSequence getStandardOutput() {
        return standardOutput;
    }

    public TestResultExt addTest(String testName, long duration,
                                 String device, String project, String flavor) {
        TestResultExt test = new TestResultExt(testName, duration, device, project, flavor, this);
        results.add(test);

        addDevice(device, test);
        addVariant(project, flavor, test);

        return addTest(test);
    }

    public void addStandardOutput(String textContent) {
        standardOutput.append(textContent);
    }

    public void addStandardError(String textContent) {
        standardError.append(textContent);
    }
}
