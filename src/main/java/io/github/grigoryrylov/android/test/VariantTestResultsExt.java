package io.github.grigoryrylov.android.test;


/**
 * Created by grishberg on 08.04.18.
 */
public class VariantTestResultsExt extends CompositeTestResultsExt {
    private final String name;

    public VariantTestResultsExt(String name, CompositeTestResultsExt parent) {
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
