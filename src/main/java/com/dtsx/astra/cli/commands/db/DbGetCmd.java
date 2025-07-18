package com.dtsx.astra.cli.commands.db;

import com.dtsx.astra.cli.core.help.Example;
import com.dtsx.astra.cli.core.output.output.OutputAll;
import com.dtsx.astra.cli.core.output.output.OutputJson;
import com.dtsx.astra.cli.core.output.table.RenderableShellTable;
import com.dtsx.astra.cli.core.output.table.ShellTable;
import com.dtsx.astra.cli.operations.db.DbGetOperation;
import com.dtsx.astra.sdk.db.domain.Database;
import com.dtsx.astra.sdk.db.domain.Datacenter;
import lombok.val;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.function.Supplier;

import static com.dtsx.astra.cli.commands.db.DbGetCmd.DbGetKeys.*;
import static com.dtsx.astra.cli.operations.db.DbGetOperation.DbGetRequest;
import static com.dtsx.astra.cli.operations.db.DbGetOperation.DbGetResult;

@Command(
    name = "get",
    aliases = { "describe" },
    description = "Get information about a specific database."
)
@Example(
    comment = "Get information about a specific database",
    command = "astra db get my_db"
)
@Example(
    comment = "Get a specific attribute of a database",
    command = "astra db get my_db --key id"
)
public class DbGetCmd extends AbstractPromptForDbCmd<DbGetResult> {
    public enum DbGetKeys {
        name,
        id,
        status,
        cloud,
        keyspace,
        keyspaces,
        region,
        regions,
        creation_time,
        vector
    }

    @Option(
        names = { "-k", "--key" },
        description = "Specific database attribute to retrieve",
        paramLabel = "KEY"
    )
    public Optional<DbGetKeys> $key;

    @Override
    public final OutputJson executeJson(Supplier<DbGetResult> result) {
        if ($key.isPresent()) {
            return execute(result);
        }
        return OutputJson.serializeValue(result.get().database());
    }

    @Override
    protected final OutputAll execute(Supplier<DbGetResult> result) {
        val dbInfo = result.get().database();

        return $key
            .map((k) -> dbInfo4Key(dbInfo, k))
            .map(OutputAll::serializeValue)
            .orElseGet(() -> this.mkTable(dbInfo));
    }

    private RenderableShellTable mkTable(Database dbInfo) {
        return ShellTable.forAttributes(new LinkedHashMap<>() {{
            put("Name", dbInfo4Key(dbInfo, name));
            put("ID", dbInfo4Key(dbInfo, id));
            put("Cloud Provider", dbInfo4Key(dbInfo, cloud));
            put("Region", dbInfo4Key(dbInfo, region));
            put("Status", dbInfo4Key(dbInfo, status));
            put("Vector", dbInfo4Key(dbInfo, vector).equals(true) ? "Enabled" : "Disabled");
            put("Default Keyspace", dbInfo4Key(dbInfo, keyspace));
            put("Creation Time", dbInfo4Key(dbInfo, creation_time));
            put("Keyspaces", dbInfo4Key(dbInfo, keyspaces));
            put("Regions", dbInfo4Key(dbInfo, regions));
        }});
    }

    private Object dbInfo4Key(Database dbInfo, DbGetKeys key) {
        return switch (key) {
            case name -> dbInfo.getInfo().getName();
            case id -> dbInfo.getId();
            case status -> dbInfo.getStatus();
            case cloud -> dbInfo.getInfo().getCloudProvider();
            case keyspace -> dbInfo.getInfo().getKeyspace();
            case keyspaces -> dbInfo.getInfo().getKeyspaces().stream().toList();
            case region -> dbInfo.getInfo().getRegion();
            case regions -> dbInfo.getInfo().getDatacenters().stream().map(Datacenter::getRegion).toList();
            case creation_time -> dbInfo.getCreationTime();
            case vector -> dbInfo.getInfo().getDbType().equals("vector");
        };
    }

    @Override
    protected DbGetOperation mkOperation() {
        return new DbGetOperation(dbGateway, new DbGetRequest($dbRef));
    }

    @Override
    protected String dbRefPrompt() {
        return "Select the database to get information about";
    }
}
