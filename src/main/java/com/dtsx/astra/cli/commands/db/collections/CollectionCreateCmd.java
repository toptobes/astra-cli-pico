package com.dtsx.astra.cli.commands.db.collections;

import com.dtsx.astra.cli.core.exceptions.AstraCliException;
import com.dtsx.astra.cli.core.help.Example;
import com.dtsx.astra.cli.core.output.output.Hint;
import com.dtsx.astra.cli.core.output.output.OutputAll;
import com.dtsx.astra.cli.operations.Operation;
import com.dtsx.astra.cli.operations.db.collection.CollectionCreateOperation;
import lombok.val;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.Map;

import static com.dtsx.astra.cli.core.exceptions.CliExceptionCode.COLLECTION_ALREADY_EXISTS;
import static com.dtsx.astra.cli.core.output.AstraColors.highlight;
import static com.dtsx.astra.cli.operations.db.collection.CollectionCreateOperation.*;

@Command(
    name = "create-collection",
    description = "Create a new Data API collection in the specified database and keyspace"
)
@Example(
    comment = "Create a basic collection",
    command = "astra db create-collection my_db -c my_collection"
)
@Example(
    comment = "Create a vector collection",
    command = "astra db create-collection my_db -c my_collection --dimension 2048"
)
@Example(
    comment = "Create a collection in a non-default keyspace",
    command = "astra db create-collection my_db -k my_keyspace -c my_collection"
)
@Example(
    comment = "Create a collection without failing if it already exists",
    command = "astra db create-collection my_db -c my_collection --if-not-exists"
)
public class CollectionCreateCmd extends AbstractCollectionSpecificCmd<CollectionCreateResult> {
    @Option(
        names = { "--if-not-exists" },
        description = { "Will create a new collection only if none with same name", DEFAULT_VALUE },
        defaultValue = "false"
    )
    public boolean $ifNotExists;

    @ArgGroup(validate = false, heading = "%nCollection configuration options:%n")
    public CollectionCreationOptions $collectionCreationOptions;

    public static class CollectionCreationOptions {
        @Option(
            names = { "-d", "--dimension" },
            paramLabel = "DIMENSION",
            description = "Dimension of the vector space for this collection",
            required = true
        )
        public Integer dimension;

        @Option(
            names = { "--default-id" },
            paramLabel = "DEFAULT_ID",
            description = "Default identifier to use for the collection"
        )
        public String defaultId;

        @Option(
            names = { "-m", "--metric" },
            paramLabel = "METRIC",
            description = "Distance metric to use for vector similarity searches"
        )
        public String metric;

        @Option(
            names = { "--indexing-allow" },
            paramLabel = "INDEXING_ALLOW",
            description = "List of attributes to add into index (comma separated)",
            split = ","
        )
        public List<String> indexingAllow;

        @Option(
            names = { "--indexing-deny" },
            paramLabel = "INDEXING_DENY",
            description = "List of attributes to remove from index (comma separated)",
            split = ","
        )
        public List<String> indexingDeny;
    }

    @ArgGroup(validate = false, heading = "%nVectorize options:%n")
    public VectorizeOptions $vectorizeOptions;

    public static class VectorizeOptions {
        @Option(
            names = { "--embedding-provider" },
            paramLabel = "EMBEDDING_PROVIDER",
            description = "Using Vectorize, embedding provider to use"
        )
        public String embeddingProvider;

        @Option(
            names = { "--embedding-model" },
            paramLabel = "EMBEDDING_MODEL",
            description = "Using Vectorize, embedding model to use"
        )
        public String embeddingModel;

        @Option(
            names = { "--embedding-key" },
            paramLabel = "EMBEDDING_KEY",
            description = "Using Vectorize, embedding key used for shared secret"
        )
        public String embeddingKey;
    }

    @Override
    public final OutputAll execute(CollectionCreateResult result) {
        return switch (result) {
            case CollectionAlreadyExists() -> handleCollectionAlreadyExists();
            case CollectionIllegallyAlreadyExists() -> throwCollectionAlreadyExists();
            case CollectionCreated() -> handleCollectionCreated();
        };
    }

    private OutputAll handleCollectionAlreadyExists() {
        val message = "Collection %s already exists in keyspace %s of database %s.".formatted(
            highlight($collRef.name()),
            highlight($keyspaceRef.name()),
            highlight($keyspaceRef.db())
        );

        val data = mkData(false);

        return OutputAll.response(message, data, List.of(
            new Hint("Get information about the existing collection:", "astra db describe-collection %s -k %s -c %s".formatted($keyspaceRef.db(), $keyspaceRef.name(), $collRef.name()))
        ));
    }

    private OutputAll handleCollectionCreated() {
        val message = "Collection %s has been created in keyspace %s of database %s.".formatted(
            highlight($collRef.name()),
            highlight($keyspaceRef.name()),
            highlight($keyspaceRef.db())
        );

        val data = mkData(true);

        return OutputAll.response(message, data, List.of(
            new Hint("Get more information about the new collection:", "astra db describe-collection %s -k %s -c %s".formatted($keyspaceRef.db(), $keyspaceRef.name(), $collRef.name()))
        ));
    }

    private <T> T throwCollectionAlreadyExists() {
        throw new AstraCliException(COLLECTION_ALREADY_EXISTS, """
          @|bold,red Error: Collection '%s' already exists in keyspace '%s' of database '%s'.|@

          To ignore this error, provide the @!--if-not-exists!@ flag to skip this error if the collection already exists.
        """.formatted(
            $collRef.name(),
            $keyspaceRef.name(),
            $keyspaceRef.db()
        ), List.of(
            new Hint("Example fix:", originalArgs(), "--if-not-exists"),
            new Hint("Get information about the existing collection:", "astra db describe-collection %s -k %s -c %s".formatted($keyspaceRef.db(), $keyspaceRef.name(), $collRef.name()))
        ));
    }

    private Map<String, Object> mkData(Boolean wasCreated) {
        return Map.of(
            "wasCreated", wasCreated
        );
    }

    @Override
    protected Operation<CollectionCreateResult> mkOperation() {
        return new CollectionCreateOperation(collectionGateway, new CollectionCreateRequest(
            $collRef,
            $collectionCreationOptions.dimension,
            $collectionCreationOptions.metric,
            $collectionCreationOptions.defaultId,
            $vectorizeOptions != null ? $vectorizeOptions.embeddingProvider : null,
            $vectorizeOptions != null ? $vectorizeOptions.embeddingModel : null,
            $vectorizeOptions != null ? $vectorizeOptions.embeddingKey : null,
            $collectionCreationOptions.indexingAllow,
            $collectionCreationOptions.indexingDeny,
            $ifNotExists
        ));
    }
}
