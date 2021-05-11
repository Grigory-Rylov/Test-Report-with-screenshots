package com.android.build.gradle.internal.test.report;

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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.Collections;
import java.util.Map;

/**
 * Extended test report with showing screenshots.
 */
public class TestReportExt {
    private final HtmlReportRenderer htmlRenderer = new HtmlReportRenderer();
    private final ReportType reportType;
    private final File resultDir;
    private final File reportDir;
    private Map<String, String> screenshotMap;

    /**
     * Simple usage of reporter.
     *
     * @param resultDir directory with generated xml after launched tests.
     * @param reportDir directory for generating html reports.
     */
    public TestReportExt(File resultDir, File reportDir) {
        this(resultDir, reportDir, Collections.emptyMap());
    }

    /**
     * @param resultDir     directory with generated xml after launched tests.
     * @param reportDir     directory for generating html reports.
     * @param screenshotMap screenshot map, contains keys : 'package + "#" + testName',
     *                      values - absolute path to screenshot file.
     */
    public TestReportExt(File resultDir, File reportDir, Map<String, String> screenshotMap) {
        this(ReportType.SINGLE_FLAVOR, resultDir, reportDir, screenshotMap);
    }

    TestReportExt(ReportType reportType, File resultDir, File reportDir,
                  Map<String, String> screenshotMap) {
        //super(reportType, resultDir, reportDir);
        this.reportType = reportType;
        this.resultDir = resultDir;
        this.reportDir = reportDir;
        this.screenshotMap = screenshotMap;
        ResourceUtils utils = new ResourceUtils();
        htmlRenderer.requireResource(utils.loadFromResources("report.js"));
        htmlRenderer.requireResource(utils.loadFromResources("base-style.css"));
        htmlRenderer.requireResource(utils.loadFromResources("style.css"));
    }

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
                NodeList ignored = testCase.getElementsByTagName("skipped");

                if (ignored.getLength() > 0) {
                    TestResultExt ignoredResult = model.addTest(className, testName, 0, deviceName, projectName, flavorName);
                    ignoredResult.ignored();
                    model.addIgnoredTest(ignoredResult);
                    continue;
                }
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
            throw new GenerateReportException(String.format("Could not load test results from '%s'.", file), e);
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
            throw new GenerateReportException(
                    String.format("Could not generate test report to '%s'.", reportDir), e);
        }
    }

    private <T extends CompositeTestResultsExt> void generatePage(T model, PageRendererExt<T> renderer,
                                                                  File outputFile) throws Exception {
        htmlRenderer.renderer(renderer).writeTo(model, outputFile);
    }

    /**
     * Regardless of the default locale, comma ('.') is used as decimal separator
     *
     * @param source
     * @return
     * @throws ParseException
     */
    public static BigDecimal parse(String source) throws ParseException {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator('.');
        DecimalFormat format = new DecimalFormat("#.#", symbols);
        format.setParseBigDecimal(true);
        return (BigDecimal) format.parse(source);
    }
}
