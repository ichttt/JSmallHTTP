package de.nocoffeetech.webservices.core.terminal;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.CommandNode;
import de.nocoffeetech.webservices.core.service.holder.ServiceHolder;
import de.nocoffeetech.webservices.core.service.holder.ServiceHolderLookup;
import de.nocoffeetech.webservices.core.terminal.api.CommandProvider;
import de.nocoffeetech.webservices.core.terminal.api.ServiceArgumentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Map;

public class BuiltinCommandProvider implements CommandProvider {
    private static final Logger LOGGER = LogManager.getLogger(BuiltinCommandProvider.class);
    private static final SimpleCommandExceptionType ALREADY_STOPPED = new SimpleCommandExceptionType(new LiteralMessage("Already stopped!"));
    private static final SimpleCommandExceptionType ALREADY_STARTED = new SimpleCommandExceptionType(new LiteralMessage("Already started!"));


    @Override
    public void registerCommand(CommandDispatcher<Void> dispatcher) {
        registerHelp(dispatcher);
        registerStart(dispatcher);
        registerStop(dispatcher);
        registerStatus(dispatcher);
    }


    public static void registerStop(CommandDispatcher<Void> dispatcher) {
        dispatcher.register(CommandProvider.literal("stop")
                .then(CommandProvider.argument("server", ServiceArgumentType.running()).executes(context -> {
                    ServiceHolder<?> service = ServiceArgumentType.getServer(context, "server");
                    try {
                        service.shutdown();
                    } catch (IllegalStateException e) {
                        throw ALREADY_STOPPED.create();
                    }
                    return Command.SINGLE_SUCCESS;

                }))
                .executes(context -> {
                    for (ServiceHolder<?> server : ServiceHolderLookup.getAll()) {
                        server.shutdownIfPossible();
                    }
                    return Command.SINGLE_SUCCESS;
                }));
    }

    public static void registerStart(CommandDispatcher<Void> dispatcher) {
        dispatcher.register(CommandProvider.literal("start")
                .then(CommandProvider.argument("server", ServiceArgumentType.stopped()).executes(context -> {
                    ServiceHolder<?> service = ServiceArgumentType.getServer(context, "server");
                    try {
                        service.startup();
                    } catch (IllegalStateException e) {
                        throw ALREADY_STARTED.create();
                    } catch (IOException e) {
                        LOGGER.error("Failed to start up server " + service, e);
                    }
                    return Command.SINGLE_SUCCESS;
                }))
                .executes(context -> {
                    for (ServiceHolder<?> server : ServiceHolderLookup.getAll()) {
                        try {
                            server.startup();
                        } catch (IllegalStateException e) {
                            //huh
                        } catch (IOException e) {
                            LOGGER.error("Failed to start up server " + server.getInstanceName(), e);
                        }
                    }
                    return Command.SINGLE_SUCCESS;
                }));
    }

    public static void registerStatus(CommandDispatcher<Void> dispatcher) {
        dispatcher.register(CommandProvider.literal("status")
                .executes(context -> {
                    LOGGER.info("---SERVER STATUS---");
                    for (ServiceHolder<?> serviceHolder : ServiceHolderLookup.getAll()) {
                        LOGGER.info("Server {} : {}", serviceHolder.getInstanceName(), serviceHolder.isRunning() ? "UP" : "DOWN");
                    }
                    return Command.SINGLE_SUCCESS;
                }));
    }

    public static void registerHelp(CommandDispatcher<Void> dispatcher) {
        dispatcher.register(CommandProvider.literal("help").executes((context) -> {
            Map<CommandNode<Void>, String> map = dispatcher.getSmartUsage(dispatcher.getRoot(), context.getSource());

            LOGGER.info("Available commands:");
            for (String s : map.values()) {
                LOGGER.info(s);
            }

            return map.size();
        }));
    }
}
