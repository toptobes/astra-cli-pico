package com.dtsx.astra.cli.commands.streaming;

import com.dtsx.astra.cli.core.datatypes.Either;
import com.dtsx.astra.cli.core.exceptions.AstraCliException;
import com.dtsx.astra.cli.core.help.Example;
import com.dtsx.astra.cli.core.models.RegionName;
import com.dtsx.astra.cli.core.models.TenantName;
import com.dtsx.astra.cli.core.models.TenantStatus;
import com.dtsx.astra.cli.core.output.output.Hint;
import com.dtsx.astra.cli.core.output.output.OutputAll;
import com.dtsx.astra.cli.operations.Operation;
import com.dtsx.astra.cli.operations.streaming.StreamingCreateOperation;
import com.dtsx.astra.sdk.db.domain.CloudProviderType;
import lombok.val;
import org.graalvm.collections.Pair;
import org.jetbrains.annotations.Nullable;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static com.dtsx.astra.cli.core.output.ExitCode.TENANT_ALREADY_EXISTS;
import static com.dtsx.astra.cli.core.output.AstraColors.highlight;
import static com.dtsx.astra.cli.operations.streaming.StreamingCreateOperation.*;

@Command(
    name = "create",
    description = "Create a new streaming tenant"
)
@Example(
    comment = "Create a basic streaming tenant in the 'us-east1' region",
    command = "astra streaming create my_tenant --region us-east1"
)
@Example(
    comment = "Create a tenant with a specific cloud provider",
    command = "astra streaming create my_tenant --region us-east1 --cloud AWS"
)
@Example(
    comment = "Create a tenant with a dedicated cluster",
    command = "astra streaming create my_tenant --cluster my_cluster"
)
@Example(
    comment = "Create a tenant if it doesn't already exist",
    command = "astra streaming create my_tenant --region us-east1 --if-not-exists"
)
public class StreamingCreateCmd extends AbstractStreamingTenantSpecificCmd<StreamingCreateResult> {
    @Option(
        names = { "--if-not-exists" },
        description = { "Don't error if the tenant already exists", DEFAULT_VALUE }
    )
    public boolean $ifNotExists;

    @ArgGroup(heading = "%nTenant configuration options:%n", multiplicity = "1")
    public TenantCreationOptions $tenantCreationOptions;

    public static class TenantCreationOptions {
        @Option(
            names = { "--plan" },
            paramLabel = "PLAN",
            description = { "Plan for the tenant", DEFAULT_VALUE },
            defaultValue = "serverless"
        )
        public String plan;

        @Option(
            names = { "-e", "--email" },
            paramLabel = "EMAIL",
            description = { "User email", DEFAULT_VALUE },
            defaultValue = "user@example.com"
        )
        public String userEmail;

        @ArgGroup(multiplicity = "1")
        public ClusterOrCloud clusterOrCloud;
    }

    public static class ClusterOrCloud {
        @Option(
            names = { "--cluster" },
            paramLabel = "CLUSTER",
            description = "Dedicated cluster, replacement for cloud/region"
        )
        public Optional<String> cluster;

        @ArgGroup(exclusive = false)
        public @Nullable RegionSpec regionSpec;
    }

    public static class RegionSpec {
        @Option(
            names = { "-r", "--region" },
            paramLabel = "REGION",
            description = "Cloud provider region to provision",
            required = true
        )
        public RegionName region;

        @Option(
            names = { "-c", "--cloud" },
            description = "The cloud provider where the tenant should be created. Inferred from the region if not provided."
        )
        public Optional<CloudProviderType> cloud;
    }

    @Override
    protected Operation<StreamingCreateResult> mkOperation() {
        return new StreamingCreateOperation(streamingGateway, new StreamingCreateRequest(
            $tenantName,
            ($tenantCreationOptions.clusterOrCloud.regionSpec != null)
                ? Either.right(Pair.create($tenantCreationOptions.clusterOrCloud.regionSpec.cloud, $tenantCreationOptions.clusterOrCloud.regionSpec.region))
                : Either.left($tenantCreationOptions.clusterOrCloud.cluster.orElseThrow()),
            $tenantCreationOptions.plan,
            $tenantCreationOptions.userEmail,
            $ifNotExists
        ));
    }

    @Override
    protected final OutputAll execute(Supplier<StreamingCreateResult> result) {
        return switch (result.get()) {
            case TenantAlreadyExistsWithStatus(var tenantName, var currStatus) -> handleTenantAlreadyExistsWithStatus(tenantName, currStatus);
            case TenantAlreadyExistsIllegallyWithStatus(var tenantName, var currStatus) -> throwTenantAlreadyExistsWithStatus(tenantName, currStatus);
            case TenantCreated(var tenantName, var currStatus) -> handleTenantCreated(tenantName, currStatus);
        };
    }

    private OutputAll handleTenantAlreadyExistsWithStatus(TenantName tenantName, TenantStatus currStatus) {
        val message = "Tenant %s already exists and has status %s.".formatted(
            highlight(tenantName),
            highlight(currStatus)
        );

        val data = mkData(tenantName, false, currStatus);

        return OutputAll.response(message, data, List.of(
            new Hint("Get information about the existing tenant:", "astra streaming get %s".formatted(tenantName))
        ));
    }

    private <T> T throwTenantAlreadyExistsWithStatus(TenantName tenantName, TenantStatus currStatus) {
        throw new AstraCliException(TENANT_ALREADY_EXISTS, """
          @|bold,red Error: Tenant %s already exists and has status %s.|@
        
          To ignore this error, provide the @!--if-not-exists!@ flag to skip this error if the tenant already exists.
        """.formatted(
            tenantName,
            currStatus
        ), List.of(
            new Hint("Example fix:", originalArgs(), "--if-not-exists"),
            new Hint("Get information about the existing tenant:", "astra streaming get %s".formatted(tenantName))
        ));
    }

    private OutputAll handleTenantCreated(TenantName tenantName, TenantStatus currStatus) {
        val message = "Tenant %s has been created.".formatted(
            highlight(tenantName)
        );

        val data = mkData(tenantName, true, currStatus);

        return OutputAll.response(message, data, List.of(
            new Hint("Get more information about the new tenant:", "astra streaming get %s".formatted(tenantName))
        ));
    }

    private Map<String, Object> mkData(TenantName tenantName, Boolean wasCreated, TenantStatus currentStatus) {
        return Map.of(
            "tenantName", tenantName,
            "wasCreated", wasCreated,
            "currentStatus", currentStatus
        );
    }
}
