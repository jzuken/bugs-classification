package org.ml_methods_group.evaluation.approaches;

import org.ml_methods_group.common.DistanceFunction;
import org.ml_methods_group.common.FeaturesExtractor;
import org.ml_methods_group.common.ast.changes.Changes;

public class ChangesApproach<F> {
    public final FeaturesExtractor<Changes, F> extractor;
    public final DistanceFunction<F> metric;
    public final String name;

    public ChangesApproach(FeaturesExtractor<Changes, F> extractor, DistanceFunction<F> metric, String name) {
        this.extractor = extractor;
        this.metric = metric;
        this.name = name;
    }
}
