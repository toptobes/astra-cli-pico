package com.dtsx.astra.cli.core.models;

import com.dtsx.astra.cli.core.datatypes.Either;
import com.dtsx.astra.cli.core.output.AstraColors;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CollectionRef implements AstraColors.Highlightable {
    private final String name;
    private final KeyspaceRef ksRef;

    public static Either<String, CollectionRef> parse(@NonNull KeyspaceRef keyspace, @NonNull String name) {
        return Utils.trimAndValidateBasics("Collection name", name)
            .map((trimmed) -> new CollectionRef(trimmed, keyspace));
    }

    @JsonValue
    public String name() {
        return name;
    }

    public KeyspaceRef keyspace() {
        return ksRef;
    }

    public DbRef db() {
        return ksRef.db();
    }

    @Override
    public String highlight() {
        return AstraColors.highlight(toString());
    }

    @Override
    public String toString() {
        return ksRef + "." + name;
    }
}
