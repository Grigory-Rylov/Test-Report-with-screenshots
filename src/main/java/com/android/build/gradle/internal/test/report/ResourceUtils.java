package com.android.build.gradle.internal.test.report;

import java.net.URL;

/**
 * Helper class for opening files from resources
 */
public class ResourceUtils {
    private final ClassLoader classLoader = getClass().getClassLoader();

    public URL loadFromResources(String name) {
        return classLoader.getResource(name);
    }
}
