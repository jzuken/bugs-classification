package org.ml_methods_group.evaluation.approaches.clustering;

import org.ml_methods_group.common.ast.changes.Changes;
import org.ml_methods_group.evaluation.approaches.ChangesApproach;
import org.ml_methods_group.evaluation.approaches.ChangesApproachTemplate;

import java.util.List;
import java.util.function.Function;

public class ChangesClusteringApproachTemplate {

    private final Function<List<Changes>, ChangesClusteringApproach> creator;

    public <T> ChangesClusteringApproachTemplate(ChangesApproachTemplate<T> template) {
        this.creator = (dataset) -> {
            final ChangesApproach<T> approach = template.getApproach(dataset);
            return new ChangesClusteringApproach(approach.name, approach);
        };
    }


    public ChangesClusteringApproach createApproach(List<Changes> train) {
        return creator.apply(train);
    }
}
