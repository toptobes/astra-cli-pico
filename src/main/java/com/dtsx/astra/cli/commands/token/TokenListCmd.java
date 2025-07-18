package com.dtsx.astra.cli.commands.token;

import com.dtsx.astra.cli.core.help.Example;
import com.dtsx.astra.cli.core.output.output.OutputAll;
import com.dtsx.astra.cli.core.output.output.OutputJson;
import com.dtsx.astra.cli.core.output.table.ShellTable;
import com.dtsx.astra.cli.operations.Operation;
import com.dtsx.astra.cli.operations.token.TokenListOperation;
import lombok.val;
import picocli.CommandLine.Command;

import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.dtsx.astra.cli.operations.token.TokenListOperation.TokenInfo;

@Command(
    name = "list",
    description = "List your tokens' information"
)
@Example(
    comment = "List your tokens' information",
    command = "astra token list"
)
public class TokenListCmd extends AbstractTokenCmd<Stream<TokenInfo>> {
    @Override
    protected OutputJson executeJson(Supplier<Stream<TokenInfo>> tokens) {
        return OutputJson.serializeValue(tokens.get().map((t) -> Map.of(
            "generatedOn", t.generatedOn(),
            "clientId", t.clientId(),
            "roleNames", t.roleNames(),
            "roleIds", t.roleIds()
        )).toList());
    }

    @Override
    public final OutputAll execute(Supplier<Stream<TokenInfo>> tokens) {
        val rows = tokens.get()
            .map(token -> Map.of(
                "Generated On", token.generatedOn(),
                "Client Id", token.clientId(),
                "Roles", token.roleNames()
            ))
            .toList();

        return new ShellTable(rows).withColumns("Generated On", "Client Id", "Roles");
    }

    @Override
    protected Operation<Stream<TokenInfo>> mkOperation() {
        return new TokenListOperation(tokenGateway, roleGateway);
    }
}
