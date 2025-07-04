package com.dtsx.astra.cli.commands;

import com.dtsx.astra.cli.config.AstraConfig.Profile;
import com.dtsx.astra.cli.config.ProfileName;
import com.dtsx.astra.cli.core.completions.impls.AstraEnvCompletion;
import com.dtsx.astra.cli.core.completions.impls.AvailableProfilesCompletion;
import com.dtsx.astra.cli.core.models.Token;
import com.dtsx.astra.cli.gateways.downloads.DownloadsGateway;
import com.dtsx.astra.sdk.utils.AstraEnvironment;
import lombok.val;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;

import java.util.Optional;

public abstract class AbstractConnectedCmd<OpRes> extends AbstractCmd<OpRes> {
    @Option(names = { "--env" }, completionCandidates = AstraEnvCompletion.class, description = "Override the target astra environment", paramLabel = "<environment>")
    private Optional<AstraEnvironment> env;

    @ArgGroup
    private @Nullable TokenProvider tokenProvider;

    public static class TokenProvider {
        @Option(names = { "--profile", "-p" }, completionCandidates = AvailableProfilesCompletion.class, description = "Specify the astrarc profile to use", paramLabel = "NAME")
        public Optional<ProfileName> profileName;

        @Option(names = { "--token" }, description = "Override the default astra token", paramLabel = "TOKEN")
        public Optional<Token> token;
    }

    private @Nullable Profile cachedProfile;

    protected DownloadsGateway downloadsGateway;

    protected final Profile profile() {
        if (cachedProfile != null) {
            return cachedProfile;
        }

        if (tokenProvider != null && tokenProvider.token.isPresent()) {
            return cachedProfile = new Profile(ProfileName.mkUnsafe("<arg_provided_token>"), tokenProvider.token.get(), env.orElse(AstraEnvironment.PROD));
        }

        val profileName = (tokenProvider != null) && tokenProvider.profileName.isPresent()
            ? tokenProvider.profileName.get()
            : ProfileName.DEFAULT;

        return cachedProfile = config().lookupProfile(profileName)
            .orElseThrow(() -> new ParameterException(spec.commandLine(), "Profile '" + profileName + "' does not exist"));
    }

    @Override
    @MustBeInvokedByOverriders
    protected void prelude() {
        super.prelude();
        downloadsGateway = DownloadsGateway.mkDefault(profile().token(), profile().env());
    }
}
