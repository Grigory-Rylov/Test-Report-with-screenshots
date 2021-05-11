package io.github.grigoryrylov.android.test;

public class GenerateReportException extends RuntimeException {
    public GenerateReportException(String message) {
        super(message);
    }

    public GenerateReportException(String message, Throwable cause) {
        super(message, cause);
    }
}
