package com.dtsx.astra.cli.commands.streaming.pulsar;

import com.dtsx.astra.cli.AstraCli;
import com.dtsx.astra.cli.commands.streaming.AbstractStreamingCmd;
import com.dtsx.astra.cli.core.exceptions.internal.misc.WindowsUnsupportedException;
import com.dtsx.astra.cli.core.output.output.OutputAll;
import com.dtsx.astra.cli.core.output.output.OutputHuman;
import com.dtsx.astra.cli.operations.streaming.pulsar.AbstractPulsarExeOperation.ConfFileCreationFailed;
import com.dtsx.astra.cli.operations.streaming.pulsar.AbstractPulsarExeOperation.Executed;
import com.dtsx.astra.cli.operations.streaming.pulsar.AbstractPulsarExeOperation.PulsarExecResult;
import com.dtsx.astra.cli.operations.streaming.pulsar.AbstractPulsarExeOperation.PulsarInstallFailed;
import org.jetbrains.annotations.MustBeInvokedByOverriders;

import java.util.function.Supplier;

public abstract class AbstractPulsarExecCmd extends AbstractStreamingCmd<PulsarExecResult> {
    @Override
    @MustBeInvokedByOverriders
    protected void prelude() {
        WindowsUnsupportedException.throwIfWindows();
        super.prelude();
    }

    @Override
    protected final OutputHuman executeHuman(Supplier<PulsarExecResult> result) {
        return switch (result.get()) {
            case PulsarInstallFailed(var msg) -> OutputAll.message(msg);
            case ConfFileCreationFailed(var msg) -> OutputAll.message(msg);
            case Executed(var exitCode) -> AstraCli.exit(exitCode);
        };
    }
}
