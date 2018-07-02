package org.ml_methods_group.core.vectorization;

import java.util.Arrays;
import java.util.List;

public class Utils {
    public void standardize(List<double[]> vectors) {
        if (vectors.isEmpty()) {
            return;
        }
        final int n = vectors.get(0).length;
        final double[] sum = new double[n];
        final double[] sumSquared = new double[n];
        for (double[] vector : vectors) {
            for (int i = 0; i < n; i++) {
                sum[i] += vector[i];
                sumSquared[i] += vector[i] * vector[i];
            }
        }
        for (int i = 0; i < n; i++) {
            final double average = sum[i] / vectors.size();
            final double variation = Math.sqrt(sumSquared[i] - average * average);
            for (double[] vector : vectors) {
                vector[i] = (vector[i] - average) / variation;
            }
        }
    }

    public void normalize(double[] vector) {
        final double norm = norm(vector);
        for (int i = 0; i < vector.length; i++) {
            vector[i] /= norm;
        }
    }

    public double norm(double[] vector) {
        return Math.sqrt(squaredNorm(vector));
    }

    public double squaredNorm(double[] vector) {
        return Arrays.stream(vector)
                .map(x -> x * x)
                .sum();
    }
}
