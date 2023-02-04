package de.nocoffeetech.webservices.core.terminal;

import com.mojang.brigadier.CommandDispatcher;
import de.nocoffeetech.webservices.core.terminal.api.CommandProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ServiceLoader;

public class CommandHandler {
    private static final Logger LOGGER = LogManager.getLogger(CommandHandler.class);
    public static final CommandDispatcher<Void> DISPATCHER = new CommandDispatcher<>();

    static {
        ServiceLoader<CommandProvider> commandProviders = ServiceLoader.load(CommandHandler.class.getModule().getLayer(), CommandProvider.class);
        for (CommandProvider commandProvider : commandProviders) {
            LOGGER.debug("Adding commands from class {}", commandProvider.getClass());
            commandProvider.registerCommand(DISPATCHER);
        }
    }
}
