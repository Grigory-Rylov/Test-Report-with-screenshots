package com.android.build.gradle.internal.test.report

import org.junit.Test

/**
 * Created by grishberg on 08.04.18.
 */
class TestReportExtTest extends GroovyTestCase {
    private HashMap<String, String> screenshotMap = new HashMap<>()

    @Test
    void testGenerateReport() {
        screenshotMap.put("com.github.grishberg.instrumentaltestsample.ExampleInstrumentedTest#failedTest1",
                "screenshots/test_phone-com.github.grishberg.instrumentaltestsample.ExampleInstrumentedTest_failedTest1.png")
        TestReport reportExt = new TestReportExt(ReportType.SINGLE_FLAVOR,
                new File("for_test"), new File("output"), screenshotMap)
        reportExt.generateReport()
    }
}
