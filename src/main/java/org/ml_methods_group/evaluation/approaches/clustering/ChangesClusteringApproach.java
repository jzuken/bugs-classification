package org.ml_methods_group.evaluation.approaches.clustering;

import org.ml_methods_group.clustering.clusterers.CompositeClusterer;
import org.ml_methods_group.clustering.clusterers.HAC;
import org.ml_methods_group.common.Clusterer;
import org.ml_methods_group.common.CommonUtils;
import org.ml_methods_group.common.Wrapper;
import org.ml_methods_group.common.ast.changes.Changes;
import org.ml_methods_group.evaluation.approaches.ChangesApproach;

import java.util.function.BiFunction;

public class ChangesClusteringApproach {

    private final BiFunction<Double, Integer, Clusterer<Changes>> creator;
    private final String name;

    public <T> ChangesClusteringApproach(String name, ChangesApproach<T> approach) {
        this.name = name;
        this.creator = (threshold, minClustersCount) -> new CompositeClusterer<>(approach.extractor, new HAC<>(
                threshold,
                minClustersCount,
                CommonUtils.metricFor(approach.metric, Wrapper::getFeatures)));
    }

    public Clusterer<Changes> getClusterer(double threshold, int minClustersCount) {
        return creator.apply(threshold, minClustersCount);
    }

    public String getName() {
        return name;
    }
}
