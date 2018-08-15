package org.ml_methods_group.core.basic.selectors;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FixedIdOptionSelector<V, K, O> implements OptionSelector<V, O> {

    private final Map<K, O> options;
    private final Function<V, K> valueIdExtractor;

    public FixedIdOptionSelector(List<O> options, Function<V, K> valueIdExtractor, Function<O, K> optionIdExtractor) {
        this.valueIdExtractor = valueIdExtractor;
        this.options = options.stream()
                .collect(Collectors.toMap(optionIdExtractor, Function.identity()));
    }

    @Override
    public Optional<O> selectOption(V value) {
        return Optional.ofNullable(options.get(valueIdExtractor.apply(value)));
    }

    @Override
    public Collection<O> getOptions() {
        return options.values();
    }
}