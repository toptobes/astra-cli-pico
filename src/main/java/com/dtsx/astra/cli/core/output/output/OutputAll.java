package com.dtsx.astra.cli.core.output.output;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public interface OutputAll extends OutputHuman, OutputJson, OutputCsv {
    static OutputAll response(CharSequence message, @Nullable Map<String, Object> data, @Nullable List<Hint> nextSteps) {
        return instance(() -> OutputHuman.response(message, nextSteps), () -> OutputJson.response(message, data, nextSteps), () -> OutputCsv.response(message, data));
    }

    static OutputAll response(CharSequence message, @Nullable Map<String, Object> data) {
        return response(message, data, null);
    }

    static OutputAll response(CharSequence message) {
        return response(message, null, null);
    }

    static OutputAll message(CharSequence s) {
        return instance(() -> OutputHuman.message(s), () -> OutputJson.message(s), () -> OutputCsv.message(s));
    }

    static OutputAll serializeValue(Object o) {
        return instance(() -> OutputHuman.serializeValue(o), () -> OutputJson.serializeValue(o), () -> OutputCsv.serializeValue(o));
    }

    static OutputAll instance(Supplier<OutputHuman> human, Supplier<OutputJson> json, Supplier<OutputCsv> csv) {
        return new OutputAll() {
            @Override
            public String renderAsHuman() {
                return human.get().renderAsHuman();
            }

            @Override
            public String renderAsJson() {
                return json.get().renderAsJson();
            }

            @Override
            public String renderAsCsv() {
                return csv.get().renderAsCsv();
            }
        };
    }

    default String render(OutputType type) {
        return switch (type) {
            case HUMAN -> renderAsHuman();
            case JSON -> renderAsJson();
            case CSV -> renderAsCsv();
        };
    }
}
