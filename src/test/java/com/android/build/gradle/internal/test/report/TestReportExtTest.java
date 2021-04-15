package com.android.build.gradle.internal.test.report;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.util.HashMap;

/**
 * Created by grishberg on 08.04.18.
 */
@RunWith(JUnit4.class)
public class TestReportExtTest {
    private HashMap<String, String> screenshotMap = new HashMap<>();

    @Test
    public void testGenerateReport() {
        screenshotMap.put("com.github.grishberg.instrumentaltestsample.ExampleInstrumentedTest#failedTest1",
                "screenshots/test_phone-com.github.grishberg.instrumentaltestsample.ExampleInstrumentedTest_failedTest1.png");
        TestReport reportExt = new TestReportExt(ReportType.SINGLE_FLAVOR,
                new File("for_test"), new File("output"), screenshotMap);
        reportExt.generateReport();
    }
}
