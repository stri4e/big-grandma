package com.github.mapper.graph;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.github.mapper.utils.MapperUtils.*;

public class DependenciesGraph {

    private static final int START_POINT = 0;

    private final Root root;

    public DependenciesGraph(Root root) {
        this.root = root;
    }

    public <T> Flux<T> many(List<Map<String, Object>> tuples) {
        return intermediateState(tuples)
                .flatMapMany(this::toTargets);
    }

    @SuppressWarnings(value = "unchecked")
    public <T> Mono<T> single(List<Map<String, Object>> tuples) {
        return (Mono<T>) intermediateState(tuples)
                .flatMapMany(groupBy -> this.toTargets(groupBy).take(1))
                .single();
    }

    private Mono<LinkedHashMap<RootRound, List<Round>>> intermediateState(List<Map<String, Object>> tuples) {
        return Flux.fromStream(tuples.stream())
                .filter(DependenciesGraph::isTupleEmpty)
                .map(tuple -> root.toRootRound(tuple, START_POINT))
                .collect(RootRoundCollector.toListOfRootRounds())
                .flatMapMany(source -> Flux.fromStream(source.stream()))
                .collect(Collectors.groupingBy(
                        Function.identity(),
                        LinkedHashMap::new,
                        Collectors.flatMapping(source -> source.nonMappedValues.stream()
                                        .flatMap(tuple -> root.graphOneToEtc.stream()
                                                .map(subGraph -> subGraph.restore(tuple, START_POINT))),
                                RoundCollector.toListOfRounds()
                        )
                ));
    }

    private <T> Flux<T> toTargetManyToMany(LinkedHashMap<RootRound, List<Round>> groupByRoot) {
        return Mono.just(groupByRoot.keySet())
                .flatMapMany(rootRounds -> {
                    List<T> result = rootRounds.stream().map(rootRound -> {
                        T target = ofTarget(rootRound, groupByRoot);
                        this.root.roundsManyToMany(rootRound);
                        return target;
                    }).collect(Collectors.toList());
                    return Flux.fromStream(result.stream());
                });
    }

    private <T> Flux<T> toTargetOneToEtc(LinkedHashMap<RootRound, List<Round>> groupByRoot) {
        return Flux.fromStream(groupByRoot.keySet().stream())
                .map(rootRound -> ofTarget(rootRound, groupByRoot));
    }

    @SuppressWarnings(value = "unchecked")
    private <T> T ofTarget(RootRound rootRound, LinkedHashMap<RootRound, List<Round>> groupByRoot) {
        Object target = rootRound.value;
        Map<String, Object> values = new HashMap<>();
        List<Round> rounds = groupByRoot.get(rootRound);
        List<SubGraph> graphs = root.graphOneToEtc;
        rounds.forEach(round ->
                graphs.forEach(graph ->
                        graph.rounds(target, round, values)
                )
        );
        mapFields(values, target);
        return (T) target;
    }

    private <T> Flux<T> toTargets(LinkedHashMap<RootRound, List<Round>> groupByRoot) {
        switch (this.root.type) {
            case oneToEtc:
                return toTargetOneToEtc(groupByRoot);
            case manyToMany:
                return toTargetManyToMany(groupByRoot);
            default:
                throw new IllegalArgumentException("Unsupported Relation type");
        }
    }

    private static boolean isTupleEmpty(Map<String, Object> values) {
        return !values.isEmpty();
    }

}
