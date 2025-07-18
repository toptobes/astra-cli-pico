package com.dtsx.astra.cli.commands.streaming;

import com.dtsx.astra.cli.core.exceptions.AstraCliException;
import com.dtsx.astra.cli.core.help.Example;
import com.dtsx.astra.cli.core.output.output.Hint;
import com.dtsx.astra.cli.core.output.output.OutputAll;
import com.dtsx.astra.cli.operations.Operation;
import com.dtsx.astra.cli.operations.streaming.StreamingDeleteOperation;
import lombok.val;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static com.dtsx.astra.cli.core.output.ExitCode.TENANT_NOT_FOUND;
import static com.dtsx.astra.cli.core.output.AstraColors.highlight;
import static com.dtsx.astra.cli.operations.streaming.StreamingDeleteOperation.*;

@Command(
    name = "delete",
    description = "Delete an existing streaming tenant"
)
@Example(
    comment = "Delete an existing streaming tenant",
    command = "astra streaming delete my_tenant"
)
@Example(
    comment = "Delete an existing streaming tenant without failing if it does not exist",
    command = "astra streaming delete my_tenant --if-exists"
)
public class StreamingDeleteCmd extends AbstractStreamingTenantSpecificCmd<StreamingDeleteResult> {
    @Option(
        names = { "--if-exists" },
        description = { "Do not fail if tenant does not exist", DEFAULT_VALUE },
        defaultValue = "false"
    )
    public boolean $ifExists;

    @Override
    protected final OutputAll execute(Supplier<StreamingDeleteResult> result) {
        return switch (result.get()) {
            case TenantNotFound() -> handleTenantNotFound();
            case TenantDeleted() -> handleTenantDeleted();
            case TenantIllegallyNotFound() -> throwTenantNotFound();
        };
    }

    private OutputAll handleTenantNotFound() {
        val message = "Tenant %s does not exist; nothing to delete.".formatted(
            highlight($tenantName)
        );

        val data = mkData(false);

        return OutputAll.response(message, data, List.of(
            new Hint("See your existing tenants:", "astra streaming list")
        ));
    }

    private OutputAll handleTenantDeleted() {
        val message = "Tenant %s has been deleted.".formatted(
            highlight($tenantName)
        );

        val data = mkData(true);

        return OutputAll.response(message, data);
    }

    private <T> T throwTenantNotFound() {
        throw new AstraCliException(TENANT_NOT_FOUND, """
          @|bold,red Error: Tenant '%s' could not be found.|@
        
          To ignore this error, you can use the @!--if-exists!@ option to avoid failing if the tenant does not exist.
        """.formatted(
            $tenantName
        ), List.of(
            new Hint("Example fix:", originalArgs(), "--if-exists"),
            new Hint("See your existing tenants:", "astra streaming list")
        ));
    }

    private Map<String, Object> mkData(Boolean wasDeleted) {
        return Map.of(
            "wasDeleted", wasDeleted
        );
    }

    @Override
    protected Operation<StreamingDeleteResult> mkOperation() {
        return new StreamingDeleteOperation(streamingGateway, new StreamingDeleteRequest($tenantName, $ifExists));
    }
}
