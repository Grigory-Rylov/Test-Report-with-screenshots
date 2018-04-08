package com.android.build.gradle.internal.test.report;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by grishberg on 08.04.18.
 */
public class AllTestResultsExt extends CompositeTestResultsExt {
    private final Map<String, PackageTestResultsExt> packages = new TreeMap<>();

    public AllTestResultsExt() {
        super(null);
    }

    @Override
    public String getTitle() {
        return "Test Summary";
    }

    public Collection<PackageTestResultsExt> getPackages() {
        return packages.values();
    }

    @Override
    public String getName() {
        return null;
    }

    public TestResultExt addTest(String className, String testName, long duration,
                                 String device, String project, String flavor) {
        PackageTestResultsExt packageResults = addPackageForClass(className);
        TestResultExt testResult = addTest(
                packageResults.addTest(className, testName, duration, device, project, flavor));

        addDevice(device, testResult);
        addVariant(project, flavor, testResult);

        return testResult;
    }

    public ClassTestResultsExt addTestClass(String className) {
        return addPackageForClass(className).addClass(className);
    }

    private PackageTestResultsExt addPackageForClass(String className) {
        String packageName;
        int pos = className.lastIndexOf(".");
        if (pos != -1) {
            packageName = className.substring(0, pos);
        } else {
            packageName = "";
        }
        return addPackage(packageName);
    }

    private PackageTestResultsExt addPackage(String packageName) {

        PackageTestResultsExt packageResults = packages.get(packageName);
        if (packageResults == null) {
            packageResults = new PackageTestResultsExt(packageName, this);
            packages.put(packageName, packageResults);
        }
        return packageResults;
    }
}
