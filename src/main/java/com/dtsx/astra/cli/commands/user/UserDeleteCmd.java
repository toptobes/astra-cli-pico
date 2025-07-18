package com.dtsx.astra.cli.commands.user;

import com.dtsx.astra.cli.core.completions.impls.UserEmailsCompletion;
import com.dtsx.astra.cli.core.exceptions.AstraCliException;
import com.dtsx.astra.cli.core.help.Example;
import com.dtsx.astra.cli.core.models.UserRef;
import com.dtsx.astra.cli.core.output.output.Hint;
import com.dtsx.astra.cli.core.output.output.OutputAll;
import com.dtsx.astra.cli.operations.Operation;
import com.dtsx.astra.cli.operations.user.UserDeleteOperation;
import lombok.val;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static com.dtsx.astra.cli.core.output.ExitCode.USER_NOT_FOUND;
import static com.dtsx.astra.cli.core.output.AstraColors.highlight;
import static com.dtsx.astra.cli.operations.user.UserDeleteOperation.*;

@Command(
    name = "delete",
    description = "Delete an existing user"
)
@Example(
    comment = "Delete a specific user",
    command = "astra user delete john@example.com"
)
@Example(
    comment = "Delete a user without failing if they doesn't exist",
    command = "astra user delete john@example.com --if-exists"
)
public class UserDeleteCmd extends AbstractUserCmd<UserDeleteResult> {
    @Parameters(
        description = "User email/id to delete",
        paramLabel = "USER",
        completionCandidates = UserEmailsCompletion.class
    )
    public UserRef user;

    @Option(
        names = { "--if-exists" },
        description = { "Do not fail if user does not exist", DEFAULT_VALUE },
        defaultValue = "false"
    )
    public boolean ifExists;

    @Override
    protected Operation<UserDeleteResult> mkOperation() {
        return new UserDeleteOperation(userGateway, new UserDeleteRequest(user, ifExists));
    }

    @Override
    public final OutputAll execute(Supplier<UserDeleteResult> result) {
        return switch (result.get()) {
            case UserDeleted() -> handleUserDeleted();
            case UserNotFound() -> handleUserNotFound();
            case UserIllegallyNotFound() -> throwUserNotFound();
        };
    }

    private OutputAll handleUserDeleted() {
        val message = "User %s has been deleted (async operation).".formatted(highlight(user));

        return OutputAll.response(message, mkData(true));
    }

    private OutputAll handleUserNotFound() {
        val message = "User %s does not exist; nothing to delete.".formatted(highlight(user));
        
        return OutputAll.response(message, mkData(false), List.of(
            new Hint("See all existing users:", "astra user list")
        ));
    }

    private <T> T throwUserNotFound() {
        throw new AstraCliException(USER_NOT_FOUND, """
          @|bold,red Error: User '%s' does not exist in this organization.|@

          This may be expected, but to avoid this error, pass the @!--if-exists!@ flag to skip this error if the user doesn't exist.
        """.formatted(
            user
        ), List.of(
            new Hint("Example fix:", originalArgs(), "--if-exists"),
            new Hint("See all existing users:", "astra user list")
        ));
    }

    private Map<String, Object> mkData(Boolean wasDeleted) {
        return Map.of(
            "wasDeleted", wasDeleted
        );
    }
}
