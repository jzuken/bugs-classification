package org.ml_methods_group.testing.selectors;

import org.ml_methods_group.common.OptionSelector;
import org.ml_methods_group.common.Repository;
import org.ml_methods_group.common.Database;

import java.util.*;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

public class CacheOptionSelector<V, O> implements OptionSelector<V, O> {

    private final Repository<String, String> repository;

    private final Map<String, O> options;
    private final OptionSelector<V, O> oracle;
    private final Function<V, String> valueIdExtractor;
    private final Function<O, String> optionIdExtractor;

    public CacheOptionSelector(OptionSelector<V, O> oracle, Database database,
                               Function<V, String> valueIdExtractor, Function<O, String> optionIdExtractor) throws Exception {
        this.options = oracle.getOptions().stream()
                .collect(Collectors.toMap(optionIdExtractor, Function.identity()));
        long hash = oracle.getOptions().stream()
                .map(optionIdExtractor)
                .sorted()
                .reduce("", (h, l) -> h +l).hashCode();
        this.repository = database.repositoryForName("option_selector@" + hash, String.class, String.class);
        this.oracle = oracle;
        this.valueIdExtractor = valueIdExtractor;
        this.optionIdExtractor = optionIdExtractor;

    }

    @Override
    public Collection<O> getOptions() {
        return options.values();
    }

    @Override
    public Optional<O> selectOption(V value) {
        final String valueId = valueIdExtractor.apply(value);
        if (valueId.equals("-1")) {
            return oracle.selectOption(value);
        }
        final Optional<O> cache = repository.loadValue(valueId).map(options::get);
        if (cache.isPresent()) {
            return cache;
        }
        final Optional<O> option = oracle.selectOption(value);
        option.map(optionIdExtractor::apply)
                .ifPresent(id -> repository.storeValue(valueId, id));
        return option;
    }
}
