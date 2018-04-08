package com.android.build.gradle.internal.test.report;

import com.google.common.io.Closeables;
import org.gradle.api.GradleException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Extended test report with showing screenshots.
 */
public class TestReportExt extends TestReport {
    private final HtmlReportRenderer htmlRenderer = new HtmlReportRenderer();
    private final ReportType reportType;
    private final File resultDir;
    private final File reportDir;
    private Map<String, String> screenshotMap;

    public TestReportExt(ReportType reportType, File resultDir, File reportDir,
                         Map<String, String> screenshotMap) {
        super(reportType, resultDir, reportDir);
        this.reportType = reportType;
        this.resultDir = resultDir;
        this.reportDir = reportDir;
        this.screenshotMap = screenshotMap;
        htmlRenderer.requireResource(getClass().getResource("report.js"));
        htmlRenderer.requireResource(getClass().getResource("base-style.css"));
        htmlRenderer.requireResource(getClass().getResource("style.css"));
    }

    @Override
    public void generateReport() {
        AllTestResultsExt model = loadModel();
        generateFiles(model);
    }

    private AllTestResultsExt loadModel() {
        AllTestResultsExt model = new AllTestResultsExt();
        if (resultDir.exists()) {
            File[] files = resultDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().startsWith("TEST-") && file.getName().endsWith(".xml")) {
                        mergeFromFile(file, model);
                    }
                }
            }
        }
        return model;
    }

    private void mergeFromFile(File file, AllTestResultsExt model) {
        InputStream inputStream = null;
        try {
            //noinspection IOResourceOpenedButNotSafelyClosed
            inputStream = new FileInputStream(file);
            Document document;
            try {
                document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                        new InputSource(inputStream));
            } finally {
                inputStream.close();
            }

            String deviceName = null;
            String projectName = null;
            String flavorName = null;
            NodeList propertiesList = document.getElementsByTagName("properties");
            for (int i = 0; i < propertiesList.getLength(); i++) {
                Element properties = (Element) propertiesList.item(i);
                XPath xPath = XPathFactory.newInstance().newXPath();
                deviceName = xPath.evaluate("property[@name='device']/@value", properties);
                projectName = xPath.evaluate("property[@name='project']/@value", properties);
                flavorName = xPath.evaluate("property[@name='flavor']/@value", properties);
            }

            NodeList testCases = document.getElementsByTagName("testcase");
            for (int i = 0; i < testCases.getLength(); i++) {
                Element testCase = (Element) testCases.item(i);
                String className = testCase.getAttribute("classname");
                String testName = testCase.getAttribute("name");
                BigDecimal duration = parse(testCase.getAttribute("time"));
                duration = duration.multiply(BigDecimal.valueOf(1000));
                NodeList failures = testCase.getElementsByTagName("failure");

                TestResultExt testResult = model.addTest(className, testName, duration.longValue(),
                        deviceName, projectName, flavorName);
                for (int j = 0; j < failures.getLength(); j++) {
                    Element failure = (Element) failures.item(j);
                    testResult.addFailure(
                            failure.getAttribute("message"), failure.getTextContent(),
                            deviceName, projectName, flavorName,
                            getScreenshotByClass(className, testName));
                }
            }
            NodeList ignoredTestCases = document.getElementsByTagName("ignored-testcase");
            for (int i = 0; i < ignoredTestCases.getLength(); i++) {
                Element testCase = (Element) ignoredTestCases.item(i);
                String className = testCase.getAttribute("classname");
                String testName = testCase.getAttribute("name");
                model.addTest(className, testName, 0, deviceName, projectName, flavorName).ignored();
            }
            String suiteClassName = document.getDocumentElement().getAttribute("name");
            ClassTestResultsExt suiteResults = model.addTestClass(suiteClassName);
            NodeList stdOutElements = document.getElementsByTagName("system-out");
            for (int i = 0; i < stdOutElements.getLength(); i++) {
                suiteResults.addStandardOutput(stdOutElements.item(i).getTextContent());
            }
            NodeList stdErrElements = document.getElementsByTagName("system-err");
            for (int i = 0; i < stdErrElements.getLength(); i++) {
                suiteResults.addStandardError(stdErrElements.item(i).getTextContent());
            }
        } catch (Exception e) {
            throw new GradleException(String.format("Could not load test results from '%s'.", file), e);
        } finally {
            try {
                Closeables.close(inputStream, true /* swallowIOException */);
            } catch (IOException e) {
                // cannot happen
            }
        }
    }

    private String getScreenshotByClass(String className, String testName) {
        String path = screenshotMap.get(className + '#' + testName);
        return path != null ? path : "";
    }

    private void generateFiles(AllTestResultsExt model) {
        try {
            generatePage(model, new OverviewPageRendererExt(reportType), new File(reportDir, "index.html"));
            for (PackageTestResultsExt packageResults : model.getPackages()) {
                generatePage(packageResults, new PackagePageRendererExt(reportType),
                        new File(reportDir, packageResults.getFilename(reportType) + ".html"));
                for (ClassTestResultsExt classResults : packageResults.getClasses()) {
                    generatePage(classResults, new ClassPageRendererExt(reportType),
                            new File(reportDir, classResults.getFilename(reportType) + ".html"));
                }
            }
        } catch (Exception e) {
            throw new GradleException(
                    String.format("Could not generate test report to '%s'.", reportDir), e);
        }
    }

    private <T extends CompositeTestResultsExt> void generatePage(T model, PageRendererExt<T> renderer,
                                                                  File outputFile) throws Exception {
        htmlRenderer.renderer(renderer).writeTo(model, outputFile);
    }
}
