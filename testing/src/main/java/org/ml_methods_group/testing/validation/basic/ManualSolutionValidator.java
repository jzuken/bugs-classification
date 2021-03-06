package org.ml_methods_group.testing.validation.basic;

import org.ml_methods_group.common.Solution;

import java.io.InputStream;
import java.io.OutputStream;

public class ManualSolutionValidator extends AbstractManualValidator<Solution, String> {

    public ManualSolutionValidator(InputStream input, OutputStream output) {
        super(input, output);
    }

    public ManualSolutionValidator() {
        this(System.in, System.out);
    }

    @Override
    protected String valueToString(Solution value) {
        return "Solution:" + System.lineSeparator() +
                "\tId: \t" + value.getId() + System.lineSeparator() +
                "\tSolution id:\t" + value.getSolutionId() + System.lineSeparator() +
                "\tVerdict:    \t" + value.getVerdict() + System.lineSeparator() +
                value.getCode() + System.lineSeparator() +
                "-----------------------------------------------------------------";
    }

    @Override
    protected String markToString(String mark) {
        return mark;
    }
}
