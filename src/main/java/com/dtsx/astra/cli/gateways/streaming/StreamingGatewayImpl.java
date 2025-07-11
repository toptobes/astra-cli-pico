package com.dtsx.astra.cli.gateways.streaming;

import com.dtsx.astra.cli.core.datatypes.CreationStatus;
import com.dtsx.astra.cli.core.datatypes.DeletionStatus;
import com.dtsx.astra.cli.core.datatypes.Either;
import com.dtsx.astra.cli.core.exceptions.internal.cli.OptionValidationException;
import com.dtsx.astra.cli.core.exceptions.internal.misc.InvalidTokenException;
import com.dtsx.astra.cli.core.models.RegionName;
import com.dtsx.astra.cli.core.models.TenantName;
import com.dtsx.astra.cli.core.output.AstraLogger;
import com.dtsx.astra.cli.gateways.APIProvider;
import com.dtsx.astra.sdk.db.domain.CloudProviderType;
import com.dtsx.astra.sdk.streaming.domain.CreateTenant;
import com.dtsx.astra.sdk.streaming.domain.StreamingRegion;
import com.dtsx.astra.sdk.streaming.domain.Tenant;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.graalvm.collections.Pair;

import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class StreamingGatewayImpl implements StreamingGateway {
    private final APIProvider apiProvider;

    @Override
    public Tenant findOne(TenantName tenantName) {
        return AstraLogger.loading("Fetching streaming tenant " + tenantName, (_) -> {
            return apiProvider.astraOpsClient().streaming().get(tenantName.unwrap());
        });
    }

    @Override
    public Stream<Tenant> findAll() {
        return AstraLogger.loading("Fetching all streaming tenants", (_) -> {
            return apiProvider.astraOpsClient().streaming().findAll();
        });
    }

    @Override
    public boolean exists(TenantName tenantName) {
        return AstraLogger.loading("Checking if streaming tenant " + tenantName + " exists", (_) -> {
            return apiProvider.astraOpsClient().streaming().exist(tenantName.unwrap());
        });
    }

    @Override
    public DeletionStatus<TenantName> delete(TenantName tenantName) {
        val exists = exists(tenantName);
        
        if (!exists) {
            return DeletionStatus.notFound(tenantName);
        }
        
        AstraLogger.loading("Deleting streaming tenant " + tenantName, (_) -> {
            apiProvider.astraOpsClient().streaming().delete(tenantName.unwrap());
            return null;
        });
        
        return DeletionStatus.deleted(tenantName);
    }

    @Override
    public SortedMap<CloudProviderType, ? extends SortedMap<String, StreamingRegionInfo>> findAllRegions() {
        return AstraLogger.loading("Fetching streaming regions", (_) -> (
            apiProvider.astraOpsClient().streaming().regions()
                .findAllServerless()
                .collect(Collectors.toMap(
                    r -> CloudProviderType.valueOf(r.getCloudProvider().toUpperCase()),
                    r -> new TreeMap<>() {{
                        put(r.getName(), new StreamingRegionInfo(r.getDisplayName(), r.getClassification().equalsIgnoreCase("premium"), r));
                    }},
                    (a, b) -> new TreeMap<>() {{
                        putAll(a);
                        putAll(b);
                    }},
                    TreeMap::new
                ))
        ));
    }

    @Override
    public Set<CloudProviderType> findAvailableClouds() {
        return AstraLogger.loading("Finding cloud providers for all available streaming regions", (_) -> (
            apiProvider.astraOpsClient().streaming().regions().findAllServerless()
                .map(StreamingRegion::getCloudProvider)
                .map(String::toUpperCase)
                .map(CloudProviderType::valueOf)
                .collect(Collectors.toSet())
        ));
    }

    @Override
    public CloudProviderType findCloudForRegion(Optional<CloudProviderType> cloud, RegionName region) {
        val cloudRegions = findAllRegions();

        if (cloud.isPresent()) {
            val cloudName = cloud.get().name().toLowerCase();

            if (!cloudRegions.containsKey(cloud.get())) {
                throw new OptionValidationException("cloud", "Cloud provider '%s' does not have any available streaming regions".formatted(cloudName));
            }

            if (!cloudRegions.get(cloud.get()).containsKey(region.unwrap().toLowerCase())) {
                throw new OptionValidationException("region", "Region '%s' is not available for cloud provider '%s'".formatted(region, cloud.get()));
            }

            return cloud.get();
        }

        val matchingClouds = cloudRegions.entrySet().stream()
            .filter(entry -> entry.getValue().containsKey(region.unwrap().toLowerCase()))
            .map(Entry::getKey)
            .toList();

        return switch (matchingClouds.size()) {
            case 0 ->
                throw new OptionValidationException("region", "Region '%s' is not available for any cloud provider".formatted(region));
            case 1 ->
                matchingClouds.getFirst();
            default ->
                throw new OptionValidationException("region", "Region '%s' is available for multiple cloud providers: %s".formatted(
                    region, matchingClouds.stream().map(CloudProviderType::name).toList()
                ));
        };
    }

    @Override
    public CreationStatus<Tenant> create(TenantName tenantName, Either<String, Pair<CloudProviderType, RegionName>> clusterOrCloud, String plan, String userEmail) {
        val exists = exists(tenantName);
        
        if (exists) {
            val tenant = findOne(tenantName);
            return CreationStatus.alreadyExists(tenant);
        }
        
        val createTenantBuilder = CreateTenant.builder()
            .tenantName(tenantName.unwrap())
            .plan(plan)
            .userEmail(userEmail);

        if (clusterOrCloud.isLeft()) {
            createTenantBuilder
                .clusterName(clusterOrCloud.getLeft());
        } else {
            createTenantBuilder
                .cloudProvider(clusterOrCloud.getRight().getLeft().name())
                .cloudRegion(clusterOrCloud.getRight().getRight().unwrap());
        }

        val createTenant = createTenantBuilder.build();
        
        AstraLogger.loading("Creating streaming tenant " + tenantName, (_) -> {
            apiProvider.astraOpsClient().streaming().create(createTenant);
            return null;
        });
        
        val newTenant = findOne(tenantName);
        return CreationStatus.created(newTenant);
    }
}
