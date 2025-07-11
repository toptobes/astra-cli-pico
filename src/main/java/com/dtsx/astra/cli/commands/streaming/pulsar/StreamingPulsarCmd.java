package com.dtsx.astra.cli.commands.streaming.pulsar;

import com.dtsx.astra.cli.core.completions.impls.TenantNamesCompletion;
import com.dtsx.astra.cli.core.datatypes.Either;
import com.dtsx.astra.cli.core.help.Example;
import com.dtsx.astra.cli.core.models.TenantName;
import com.dtsx.astra.cli.operations.Operation;
import com.dtsx.astra.cli.operations.streaming.pulsar.AbstractPulsarExeOperation.PulsarExecResult;
import com.dtsx.astra.cli.operations.streaming.pulsar.StreamingPulsarOperation;
import com.dtsx.astra.cli.operations.streaming.pulsar.StreamingPulsarOperation.PulsarRequest;
import org.jetbrains.annotations.Nullable;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.util.Optional;

@Command(
    name = "pulsar",
    description = "Launch Apache Pulsar for a streaming tenant"
)
@Example(
    comment = "Launch pulsar for a tenant",
    command = "astra streaming pulsar my_tenant"
)
public class StreamingPulsarCmd extends AbstractPulsarExecCmd {
    @Parameters(
        paramLabel = "TENANT",
        description = "The name of the tenant to connect to",
        completionCandidates = TenantNamesCompletion.class
    )
    public TenantName $tenantName;

    @Option(
        names = { "--pulsar-version" },
        description = "Display cqlsh version information"
    )
    public boolean $version;

    @ArgGroup
    public @Nullable Exec $exec;

    public static class Exec {
        @Option(
            names = { "-e", "--execute" },
            paramLabel = "COMMAND",
            description = "Execute the statement and quit"
        )
        public Optional<String> $execute;

        @Option(
            names = { "-f", "--filename" },
            description = "Input filename with a list of commands to be executed. Each command must be separated by a newline",
            paramLabel = "FILE"
        )
        public Optional<File> $commandsFile;
    }

    @Option(
        names = { "-F", "--fail-on-error" },
        description = { "Interrupt the shell if a command throws an exception", DEFAULT_VALUE }
    )
    public boolean $failOnError;

    @Option(
        names = { "-np", "--no-progress" },
        description = "Display raw output of the commands without progress visualization"
    )
    public boolean $noProgress;

    @Override
    protected Operation<PulsarExecResult> mkOperation() {
        return new StreamingPulsarOperation(streamingGateway, downloadsGateway, new PulsarRequest(
            $tenantName,
            $version,
            $failOnError,
            Optional.ofNullable($exec).map(e -> e.$execute.<Either<String, File>>map(Either::left).orElseGet(() -> Either.right(e.$commandsFile.orElseThrow()))),
            $noProgress
        ));
    }
}
