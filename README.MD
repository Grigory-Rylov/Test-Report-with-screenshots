Расширение `com.android.build.gradle.internal.test.report.TestReport`
с возможностью отображать скриншоты.
Для этого в конструктор необходимо передать `Map<String, String> screenshots`
Ключ представляет собой строку `package + "#" + testName`,
значение - относительный путь до скриншота (не должно содержать '#' )

# Пример использования:
```
 val report = TestReportExt(
            resultsDir,
            reportsDir,
            screenshotRelations)
 report.generateReport()
```

где:
- **resultsDir** - папка со сгенеренованными xml с результатами прогона инструментальных тестов
- **reportsDir** - папка в которой будет сгенерирован html отчет на основе xml
- **screenshotRelations** - `Map<String, String>` с привязками тестов к скриншотам. (опциональный параметр)


# Импорт
**Gradle:**

```
dependencies {
    implementation "io.github.grigory-rylov:test-report:1.5.0"
}
```