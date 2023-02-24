package de.nocoffeetech.webservices.core.terminal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.nocoffeetech.webservices.core.terminal.api.CommandProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CommandHandler {
    private static final Logger LOGGER = LogManager.getLogger(CommandHandler.class);
    public static final CommandDispatcher<Void> DISPATCHER = new CommandDispatcher<>();
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r);
        thread.setName("CommandExecutor Thread");
        thread.setDaemon(true);
        return thread;
    });

    static {
        ServiceLoader<CommandProvider> commandProviders = ServiceLoader.load(CommandHandler.class.getModule().getLayer(), CommandProvider.class);
        for (CommandProvider commandProvider : commandProviders) {
            LOGGER.debug("Adding commands from class {}", commandProvider.getClass());
            commandProvider.registerCommand(DISPATCHER);
        }
    }

    public static void executeCommand(String commandLine) {
        commandLine = commandLine.trim();
        if (commandLine.isEmpty()) {
            return;
        }
        StringReader stringreader = new StringReader(commandLine);
        if (stringreader.canRead() && stringreader.peek() == '/') {
            stringreader.skip();
        }
        EXECUTOR.submit(() -> {
            try {
                CommandHandler.DISPATCHER.execute(stringreader, null);
            } catch (CommandSyntaxException e) {
                LOGGER.error(e.getMessage());
            }
        });
    }
}
