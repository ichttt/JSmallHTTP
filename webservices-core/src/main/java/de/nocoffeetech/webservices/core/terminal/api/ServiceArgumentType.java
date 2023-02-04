package de.nocoffeetech.webservices.core.terminal.api;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import de.nocoffeetech.webservices.core.service.holder.ServiceHolder;
import de.nocoffeetech.webservices.core.service.holder.ServiceHolderLookup;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class ServiceArgumentType implements ArgumentType<ServiceHolder<?>> {
    private static final SimpleCommandExceptionType INVALID_SERVER = new SimpleCommandExceptionType(new LiteralMessage("Could not find that server!"));
    private final boolean excludeRunning;
    private final boolean excludeStopped;

    private ServiceArgumentType(boolean excludeRunning, boolean excludeStopped) {
        this.excludeRunning = excludeRunning;
        this.excludeStopped = excludeStopped;
    }

    public static ServiceArgumentType running() {
        return new ServiceArgumentType(false, true);
    }

    public static ServiceArgumentType stopped() {
        return new ServiceArgumentType(true, false);
    }

    public static ServiceArgumentType all() {
        return new ServiceArgumentType(false, false);
    }

    public static ServiceHolder<?> getServer(final CommandContext<?> context, final String name) {
        return context.getArgument(name, ServiceHolder.class);
    }

    private Stream<ServiceHolder<?>> createServiceStream() {
        Stream<ServiceHolder<?>> allowedServicesStream = ServiceHolderLookup.getAll().stream();
        if (excludeRunning)
            allowedServicesStream = allowedServicesStream.filter(serviceHolder -> !serviceHolder.isRunning());
        if (excludeStopped)
            allowedServicesStream = allowedServicesStream.filter(ServiceHolder::isRunning);
        return allowedServicesStream;
    }

    @Override
    public ServiceHolder<?> parse(StringReader reader) throws CommandSyntaxException {
        String base = reader.readUnquotedString();
        Optional<ServiceHolder<?>> service = createServiceStream().filter(serviceHolder -> serviceHolder.getInstanceName().equalsIgnoreCase(base)).findFirst();
        if (service.isEmpty()) {
            throw INVALID_SERVER.create();
        }
        return service.get();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        Stream<ServiceHolder<?>> allowedServicesStream = createServiceStream();
        String remaining = builder.getRemainingLowerCase();
        allowedServicesStream
                .map(ServiceHolder::getInstanceName)
                .map(s -> s.toLowerCase(Locale.ROOT))
                .filter(s -> s.startsWith(remaining))
                .forEach(builder::suggest);

        return builder.buildFuture();
    }
}
