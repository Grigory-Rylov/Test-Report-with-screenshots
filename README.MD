Расширение `com.android.build.gradle.internal.test.report.TestReport`
с возможностью отображать скриншоты.
Для этого в конструктор необходимо передать `Map<String, String> screenshots`
Ключ представляет собой строку `package + "#" + testName`,
значение - относительный путь до скриншота (не должно содержать '#' )