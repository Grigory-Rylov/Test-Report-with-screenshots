package io.github.grigoryrylov.android.test;

import org.gradle.fork.api.tasks.testing.TestResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by grishberg on 08.04.18.
 */
class ClassPageRendererExt extends PageRendererExt<ClassTestResultsExt> {
    private final CodePanelRenderer codePanelRenderer = new CodePanelRenderer();

    ClassPageRendererExt(ReportType reportType) {
        super(reportType);
    }

    @Override
    protected String getTitle() {
        return getModel().getTitle();
    }

    @Override
    protected void renderBreadcrumbs(SimpleHtmlWriter htmlWriter) throws IOException {
        htmlWriter.startElement("div").attribute("class", "breadcrumbs")
                .startElement("a").attribute("href", "index.html").characters("all").endElement()
                .characters(" > ")
                .startElement("a").attribute("href", String.format("%s.html", getResults().getPackageResults().getFilename(reportType))).characters(getResults().getPackageResults().getName()).endElement()
                .characters(String.format(" > %s", getResults().getSimpleName()))
                .endElement();
    }

    private void renderTests(SimpleHtmlWriter htmlWriter) throws IOException {
        htmlWriter.startElement("table")
                .startElement("thead")
                .startElement("tr")
                .startElement("th").characters("Test").endElement();

        // get all the results per device and per test name
        Map<String, Map<String, TestResultExt>> results = getResults().getTestResultsMap();

        // gather all devices.
        List<String> devices = new ArrayList(results.keySet());
        Collections.sort(devices);

        for (String device : devices) {
            htmlWriter.startElement("th").characters(device).endElement();
        }
        htmlWriter.endElement().endElement(); // tr/thead

        // gather all tests
        Set<String> tests = new HashSet<>();
        for (Map<String, TestResultExt> deviceMap : results.values()) {
            tests.addAll(deviceMap.keySet());
        }
        List<String> sortedTests = new ArrayList<>(tests);
        Collections.sort(sortedTests);

        for (String testName : sortedTests) {
            htmlWriter.startElement("tr").startElement("td").characters(testName).endElement();

            TestResult.ResultType currentType = TestResult.ResultType.SKIPPED;

            // loop for all devices to find this test and put its result
            for (String device : devices) {
                Map<String, TestResultExt> deviceMap = results.get(device);
                TestResultExt test = deviceMap.get(testName);
                if (test != null) {
                    htmlWriter.startElement("td").attribute("class", test.getStatusClass())
                            .characters(String.format("%s (%s)",
                                    test.getFormattedResultType(), test.getFormattedDuration()))
                            .endElement();

                    currentType = combineResultType(currentType, test.getResultType());
                } else {
                    htmlWriter.startElement("td").characters("not run ").endElement();
                }
            }

            // finally based on whether if a single test failed, set the class on the test name.
//todo            td.setAttribute("class", getStatusClass(currentType));

            htmlWriter.endElement(); //tr
        }
        htmlWriter.endElement(); // table
    }

    public static TestResult.ResultType combineResultType(TestResult.ResultType currentType, TestResult.ResultType newType) {
        switch (currentType) {
            case SUCCESS:
                if (newType == TestResult.ResultType.FAILURE) {
                    return newType;
                }

                return currentType;
            case FAILURE:
                return currentType;
            case SKIPPED:
                if (newType != TestResult.ResultType.SKIPPED) {
                    return newType;
                }
                return currentType;
            default:
                throw new IllegalStateException();
        }
    }

    public String getStatusClass(TestResult.ResultType resultType) {
        switch (resultType) {
            case SUCCESS:
                return "success";
            case FAILURE:
                return "failures";
            case SKIPPED:
                return "skipped";
            default:
                throw new IllegalStateException();
        }
    }

    private static final class TestPercent {
        int failed;
        int total;

        TestPercent(int failed, int total) {
            this.failed = failed;
            this.total = total;
        }

        boolean isFullFailure() {
            return failed == total;
        }
    }

    @Override
    protected void renderFailures(SimpleHtmlWriter htmlWriter) throws IOException {
        // get all the results per device and per test name
        Map<String, Map<String, TestResultExt>> results = getResults().getTestResultsMap();

        Map<String, ClassPageRendererExt.TestPercent> testPassPercent = new HashMap<>();

        for (TestResultExt test : getResults().getFailures()) {
            String testName = test.getName();
            // compute the display name which will include the name of the device and how many
            // devices are impact so to not force counting.
            // If all devices, then we don't display all of them.
            // (The off chance that all devices fail the test with a different stack trace is slim)
            ClassPageRendererExt.TestPercent percent = testPassPercent.get(testName);
            if (percent != null && percent.isFullFailure()) {
                continue;
            }

            if (percent == null) {
                int failed = 0;
                int total = 0;
                for (Map<String, TestResultExt> deviceMap : results.values()) {
                    TestResultExt testResult = deviceMap.get(testName);
                    if (testResult == null) {
                        continue;
                    }
                    TestResult.ResultType resultType = testResult.getResultType();

                    if (resultType == TestResult.ResultType.FAILURE) {
                        failed++;
                    }

                    if (resultType != TestResult.ResultType.SKIPPED) {
                        total++;
                    }
                }

                percent = new ClassPageRendererExt.TestPercent(failed, total);
                testPassPercent.put(testName, percent);
            }

            String name;
            if (percent.total == 1) {
                name = testName;
            } else if (percent.isFullFailure()) {
                name = testName + " [all devices]";
            } else {
                name = String.format("%s [%s] (on %d/%d devices)", testName, test.getDevice(),
                        percent.failed, percent.total);
            }

            htmlWriter.startElement("div").attribute("class", "test")
                    .startElement("a").attribute("name", test.getId().toString()).characters("").endElement() //browsers dont understand <a name="..."/>
                    .startElement("h3").attribute("class", test.getStatusClass()).characters(name).endElement();
            for (TestResultExt.TestFailure failure : test.getFailures()) {
                if (failure.getScreenshotPath().length() > 0) {
                    htmlWriter.startElement("div").attribute("class", "screenshot")
                            .startElement("a").attribute("href", failure.getScreenshotPath())
                                .attribute("target", "_blank")
                            .characters("screenshot").endElement()
                            .endElement();
                }

                codePanelRenderer.render(failure.getStackTrace(), htmlWriter);
            }
            htmlWriter.endElement();
        }
    }

    @Override
    protected void registerTabs() {
        addFailuresTab();
        addTab("Tests", new ErroringAction<SimpleHtmlWriter>() {
            @Override
            public void doExecute(SimpleHtmlWriter writer) throws IOException {
                renderTests(writer);
            }
        });
        addDeviceAndVariantTabs();
    }
}
