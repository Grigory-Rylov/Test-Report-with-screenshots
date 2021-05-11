package io.github.grigoryrylov.android.test;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by grishberg on 08.04.18.
 */
public class PackageTestResultsExt extends CompositeTestResultsExt {
    private static final String DEFAULT_PACKAGE = "default-package";
    private final String name;
    private final Map<String, ClassTestResultsExt> classes = new TreeMap<>();

    public PackageTestResultsExt(String name, AllTestResultsExt model) {
        super(model);
        this.name = name.isEmpty() ? DEFAULT_PACKAGE : name;
    }

    @Override
    public String getTitle() {
        return name.equals(DEFAULT_PACKAGE) ? "Default package" : String.format("Package %s", name);
    }

    @Override
    public String getName() {
        return name;
    }

    public Collection<ClassTestResultsExt> getClasses() {
        return classes.values();
    }

    public TestResultExt addTest(String className, String testName, long duration,
                                 String device, String project, String flavor) {
        ClassTestResultsExt classResults = addClass(className);
        TestResultExt testResult = addTest(
                classResults.addTest(testName, duration, device, project, flavor));

        addDevice(device, testResult);
        addVariant(project, flavor, testResult);

        return testResult;
    }

    public ClassTestResultsExt addClass(String className) {
        ClassTestResultsExt classResults = classes.get(className);
        if (classResults == null) {
            classResults = new ClassTestResultsExt(className, this);
            classes.put(className, classResults);
        }
        return classResults;
    }
}
