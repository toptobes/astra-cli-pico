package com.dtsx.astra.cli.core.models;

import com.dtsx.astra.cli.core.datatypes.Either;
import com.dtsx.astra.cli.core.output.AstraColors;
import com.dtsx.astra.cli.core.output.AstraColors.Highlightable;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.graalvm.collections.Pair;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CdcRef implements Highlightable {
    private final DbRef dbRef;
    private final Either<CdcId, Pair<TableRef, TenantName>> ref;

    public static CdcRef fromId(DbRef dbRef, CdcId id) {
        return new CdcRef(dbRef, Either.left(id));
    }

    public static CdcRef fromDefinition(TableRef tableRef, TenantName tenantName) {
        return new CdcRef(tableRef.db(), Either.right(Pair.create(tableRef, tenantName)));
    }

    public boolean isId() {
        return ref.isLeft();
    }

    public boolean isDefinition() {
        return ref.isRight();
    }

    public DbRef db() {
        return dbRef;
    }

    public <T> T fold(Function<CdcId, T> idMapper, BiFunction<TableRef, TenantName, T> defMapper) {
        return ref.fold(
            idMapper,
            pair -> defMapper.apply(pair.getLeft(), pair.getRight())
        );
    }

    @JsonValue
    public Map<String, Object> toJson() {
        return ref.fold(
            id -> Map.of("type", "id", "unwrap", id.toString()),
            ref -> Map.of("type", "ref", "unwrap", Map.of("table", ref.getLeft(), "tenant", ref.getRight()))
        );
    }

    @Override
    public String toString() {
        return fold(CdcId::toString, "(table=%s,tenant=%s)"::formatted);
    }

    @Override
    public String highlight() {
        return AstraColors.highlight(toString());
    }
}
