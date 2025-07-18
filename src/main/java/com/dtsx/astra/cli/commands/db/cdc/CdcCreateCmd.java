package com.dtsx.astra.cli.commands.db.cdc;

import com.dtsx.astra.cli.core.completions.impls.TenantNamesCompletion;
import com.dtsx.astra.cli.core.exceptions.AstraCliException;
import com.dtsx.astra.cli.core.exceptions.internal.cli.OptionValidationException;
import com.dtsx.astra.cli.core.help.Example;
import com.dtsx.astra.cli.core.models.KeyspaceRef;
import com.dtsx.astra.cli.core.models.TableRef;
import com.dtsx.astra.cli.core.models.TenantName;
import com.dtsx.astra.cli.core.output.output.Hint;
import com.dtsx.astra.cli.core.output.output.OutputAll;
import com.dtsx.astra.cli.operations.Operation;
import com.dtsx.astra.cli.operations.db.cdc.CdcCreateOperation;
import lombok.val;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static com.dtsx.astra.cli.core.output.AstraColors.highlight;
import static com.dtsx.astra.cli.core.output.ExitCode.*;
import static com.dtsx.astra.cli.operations.db.cdc.CdcCreateOperation.*;

@Command(
    name = "create-cdc",
    description = "Create a CDC (Change Data Capture) for a table in a specific keyspace and tenant."
)
@Example(
    comment = "Create a CDC",
    command = "astra db create-cdc my_db -t my_table --tenant my_tenant"
)
@Example(
    comment = "Create a CDC with a specific keyspace and topic partitions",
    command = "astra db create-cdc my_db -k my_keyspace -t my_table --tenant my_tenant --topic-partitions 5"
)
@Example(
    comment = "Create a CDC without failing if it already exists",
    command = "astra db create-cdc my_db -t my_table --tenant my_tenant --if-not-exists"
)
public class CdcCreateCmd extends AbstractCdcCmd<CdcCreateResult> {
    @Option(
        names = { "--keyspace", "-k" },
        description = { "Keyspace where the table resides", DEFAULT_VALUE },
        paramLabel = "KEYSPACE",
        defaultValue = "default_keyspace"
    )
    public String $keyspace;

    @Option(
        names = { "--table", "-t" },
        description = { "The table or collection to create CDC for", DEFAULT_VALUE },
        paramLabel = "TABLE",
        required = true
    )
    public String $table;

    @Option(
        names = { "--tenant" },
        description = { "The tenant name", DEFAULT_VALUE },
        paramLabel = "TENANT",
        completionCandidates = TenantNamesCompletion.class,
        required = true
    )
    public TenantName $tenant;

    @Option(
        names = { "--topic-partitions" },
        description = { "Number of topic partitions", DEFAULT_VALUE },
        paramLabel = "PARTITIONS",
        defaultValue = "3"
    )
    public int $topicPartitions;

    @Option(
        names = { "--if-not-exists" },
        description = { "Will create a new CDC only if none exists", DEFAULT_VALUE },
        defaultValue = "false"
    )
    public boolean $ifNotExists;

    @Override
    protected void prelude() {
        super.prelude();
        throw new AstraCliException(UNSUPPORTED_EXECUTION, "Creating CDCs via the Astra CLI is not currently not supported due to a dependency bug.");
    }

    @Override
    public final OutputAll execute(Supplier<CdcCreateResult> result) {
        return switch (result.get()) {
            case CdcAlreadyExists() -> handleCdcAlreadyExists();
            case CdcIllegallyAlreadyExists() -> throwCdcAlreadyExists();
            case CdcCreated() -> handleCdcCreated();
        };
    }

    private OutputAll handleCdcAlreadyExists() {
        val msg = """
          CDC already exists for table %s with tenant %s in database %s.
        """.formatted(
            highlight(tableRef().toString()),
            highlight($tenant),
            highlight($dbRef)
        );

        return OutputAll.response(msg, mkData(false), List.of(
            new Hint("List existing CDCs:", "astra db list-cdcs " + $dbRef)
        ));
    }

    private OutputAll handleCdcCreated() {
        val msg = """
          CDC has been created for table %s with tenant %s in database %s.
        """.formatted(
            highlight(tableRef().toString()),
            highlight($tenant),
            highlight($dbRef)
        );

        return OutputAll.response(msg, mkData(true));
    }

    private <T> T throwCdcAlreadyExists() {
        val tableRef = tableRef();
        
        throw new AstraCliException(CDC_ALREADY_EXISTS, """
          @|bold,red Error: CDC already exists for table '%s' with tenant '%s' in database '%s'.|@

          To ignore this error, provide the @!--if-not-exists!@ flag to skip this error if the CDC already exists.
        """.formatted(
            tableRef.toString(),
            $tenant,
            $dbRef
        ), List.of(
            new Hint("Example fix:", originalArgs(), "--if-not-exists"),
            new Hint("List existing CDCs:", "astra db list-cdcs " + $dbRef)
        ));
    }

    private Map<String, Object> mkData(Boolean wasCreated) {
        return Map.of(
            "wasCreated", wasCreated
        );
    }

    @Override
    protected Operation<CdcCreateResult> mkOperation() {
        return new CdcCreateOperation(cdcGateway, new CdcCreateRequest(
            tableRef(),
            $tenant,
            $topicPartitions,
            $ifNotExists
        ));
    }

    private TableRef tableRef() {
        val keyspaceRef = KeyspaceRef.parse($dbRef, $keyspace).getRight((msg) -> {
            throw new OptionValidationException("keyspace", msg);
        });

        return TableRef.parse(keyspaceRef, $table).getRight((msg) -> {
            throw new OptionValidationException("table", msg);
        });
    }
}
