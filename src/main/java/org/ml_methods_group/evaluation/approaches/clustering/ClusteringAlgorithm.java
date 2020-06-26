package org.ml_methods_group.evaluation.approaches.clustering;

import org.ml_methods_group.common.Clusterer;
import org.ml_methods_group.common.ast.changes.Changes;
import org.ml_methods_group.evaluation.approaches.BOWApproach;
import org.ml_methods_group.evaluation.approaches.FuzzyJaccardApproach;
import org.ml_methods_group.evaluation.approaches.JaccardApproach;
import org.ml_methods_group.evaluation.approaches.VectorizationApproach;

import javax.ws.rs.NotSupportedException;
import java.util.List;

public enum ClusteringAlgorithm {

    FuzzyJaccard("fuz_jac", new ChangesClusteringApproachTemplate((dataset) ->
            FuzzyJaccardApproach.getDefaultApproach())),

    DefaultJaccard("jac", new ChangesClusteringApproachTemplate((dataset) ->
            JaccardApproach.getDefaultApproach())),

    ExtendedJaccard("ext_jac", new ChangesClusteringApproachTemplate((dataset) ->
            JaccardApproach.getExtendedApproach())),

    FullJaccard("full_jac", new ChangesClusteringApproachTemplate((dataset) ->
            JaccardApproach.getFullApproach())),

    BagOfWords("bow", new ChangesClusteringApproachTemplate((dataset) ->
            BOWApproach.getDefaultApproach(20000, dataset))),

    Vectorization("vec", new ChangesClusteringApproachTemplate(VectorizationApproach::getDefaultApproach));

    String code;
    ChangesClusteringApproachTemplate template;

    ClusteringAlgorithm(String code, ChangesClusteringApproachTemplate template) {
        this.code = code;
        this.template = template;
    }

    public String getCode() {
        return code;
    }

    public static ClusteringAlgorithm getAlgorithmByCode(String code) {
        for (ClusteringAlgorithm algorithm : values()) {
            if (algorithm.getCode().equals(code)) {
                return algorithm;
            }
        }
        throw new NotSupportedException("Algorithm " + code + " is not supported");
    }

    public Clusterer<Changes> getClusterer(List<Changes> changes, double distanceLimit, int minClustersCount) {
        return template.createApproach(changes).getClusterer(distanceLimit, minClustersCount);
    }
}

