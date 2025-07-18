package com.dtsx.astra.cli.commands.db;

import com.dtsx.astra.cli.commands.db.cdc.CdcCreateCmd;
import com.dtsx.astra.cli.commands.db.cdc.CdcDeleteCmd;
import com.dtsx.astra.cli.commands.db.cdc.CdcListCmd;
import com.dtsx.astra.cli.commands.db.collections.*;
import com.dtsx.astra.cli.commands.db.cqlsh.DbCqlshCmd;
import com.dtsx.astra.cli.commands.db.dsbulk.DbCountCmd;
import com.dtsx.astra.cli.commands.db.dsbulk.DbLoadCmd;
import com.dtsx.astra.cli.commands.db.dsbulk.DbUnloadCmd;
import com.dtsx.astra.cli.commands.db.misc.EmbeddingProvidersListCmd;
import com.dtsx.astra.cli.commands.db.table.TableDeleteCmd;
import com.dtsx.astra.cli.commands.db.table.TableDescribeCmd;
import com.dtsx.astra.cli.commands.db.table.TableListCmd;
import com.dtsx.astra.cli.commands.db.table.TableTruncateCmd;
import com.dtsx.astra.cli.commands.db.endpoints.EndpointApiCmd;
import com.dtsx.astra.cli.commands.db.endpoints.EndpointPlaygroundCmd;
import com.dtsx.astra.cli.commands.db.endpoints.EndpointSwaggerCmd;
import com.dtsx.astra.cli.commands.db.keyspace.KeyspaceCreateCmd;
import com.dtsx.astra.cli.commands.db.keyspace.KeyspaceDeleteCmd;
import com.dtsx.astra.cli.commands.db.keyspace.KeyspaceListCmd;
import com.dtsx.astra.cli.commands.db.misc.CloudsListCmd;
import com.dtsx.astra.cli.commands.db.region.*;
import com.dtsx.astra.cli.core.help.Example;
import picocli.CommandLine.Command;

@Command(
    name = "db",
    description = "Manage your Astra databases",
    subcommands = {
        DbListCmd.class,
        DbGetCmd.class,
        DbCreateCmd.class,
        DbDeleteCmd.class,
        DbStatusCmd.class,
        DbCqlshCmd.class,
        DbCountCmd.class,
        DbLoadCmd.class,
        DbUnloadCmd.class,
        DbCreateDotEnv.class,
        DbDownloadScbCmd.class,
        DbResumeCmd.class,
        KeyspaceListCmd.class,
        KeyspaceCreateCmd.class,
        KeyspaceDeleteCmd.class,
        CollectionListCmd.class,
        CollectionCreateCmd.class,
        CollectionDescribeCmd.class,
        CollectionDeleteCmd.class,
        CollectionTruncateCmd.class,
        TableListCmd.class,
        TableDescribeCmd.class,
        TableDeleteCmd.class,
        TableTruncateCmd.class,
        EmbeddingProvidersListCmd.class,
        CdcListCmd.class,
        CdcCreateCmd.class,
        CdcDeleteCmd.class,
        RegionCreateCmd.class,
        RegionDeleteCmd.class,
        RegionListCmd.class,
        RegionListClassicCmd.class,
        RegionListServerlessCmd.class,
        RegionListVectorCmd.class,
        CloudsListCmd.class,
        EndpointSwaggerCmd.class,
        EndpointApiCmd.class,
        EndpointPlaygroundCmd.class,
    }
)
@Example(
    comment = "List all your Astra databases",
    command = "astra db"
)
@Example(
    comment = "List only vector-enabled Astra databases",
    command = "astra db --vector"
)
public final class DbCmd extends DbListImpl {}
