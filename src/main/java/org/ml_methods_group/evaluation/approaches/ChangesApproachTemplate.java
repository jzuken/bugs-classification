package org.ml_methods_group.evaluation.approaches;

import org.ml_methods_group.common.ast.changes.Changes;

import java.util.List;

public interface ChangesApproachTemplate<T> {
    ChangesApproach<T> getApproach(List<Changes> dataset);
}