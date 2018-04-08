package com.android.build.gradle.internal.test.report;

import com.android.annotations.NonNull;

/**
 * Created by grishberg on 08.04.18.
 */
public class VariantTestResultsExt extends CompositeTestResultsExt {
    private final String name;

    public VariantTestResultsExt(@NonNull String name, CompositeTestResultsExt parent) {
        super(parent);
        this.name = name;
    }

    @Override
    public String getTitle() {
        return name;
    }

    @Override
    public String getName() {
        return name;
    }
}
