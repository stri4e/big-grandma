package com.github.mapper.graph.cases;

import com.github.mapper.graph.cases.impl.LevelOneOnlyOneToOne;
import com.github.mapper.graph.cases.impl.ManyToManyAuthorBook;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.util.stream.Stream;

public class DependenciesGraphProvider implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
        return Stream.of(
//                Arguments.of(new LevelOneOnlyOneToOne()),
                Arguments.of(new ManyToManyAuthorBook())
        );
    }
}
