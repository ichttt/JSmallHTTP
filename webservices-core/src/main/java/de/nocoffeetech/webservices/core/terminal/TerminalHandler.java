package de.nocoffeetech.webservices.core.terminal;

import de.nocoffeetech.webservices.core.service.holder.ServiceHolder;
import de.nocoffeetech.webservices.core.service.holder.ServiceHolderLookup;
import net.minecrell.terminalconsole.TerminalConsoleAppender;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;

import java.util.Objects;

public class TerminalHandler implements Runnable {
    private static final Logger LOGGER = LogManager.getLogger(TerminalHandler.class);
    private final Terminal terminal;
    private final String appName;

    public static void setup() {
        //Try to set up console command handler
        Terminal terminal = TerminalConsoleAppender.getTerminal();
        if (terminal == null) {
            LOGGER.info("Terminal not present, skipping terminal handler...");
        } else {
            LOGGER.debug("Starting terminal handler");
            Thread terminalHandler = new Thread(new TerminalHandler(terminal, "Terminal"));
            terminalHandler.setDaemon(false);
            terminalHandler.setName("TerminalHandler Thread");
            terminalHandler.start();
        }
    }

    private TerminalHandler(Terminal terminal, String appName) {
        this.terminal = Objects.requireNonNull(terminal);
        this.appName = Objects.requireNonNull(appName);
    }

    @Override
    public void run() {
        LineReader reader = LineReaderBuilder.builder()
                .appName(appName)
                .terminal(terminal)
                .completer(new CommandCompleter())
                .build();
        reader.setOpt(LineReader.Option.DISABLE_EVENT_EXPANSION);
        reader.unsetOpt(LineReader.Option.INSERT_TAB);

        TerminalConsoleAppender.setReader(reader);
        LOGGER.info("Terminal reader setup");

        try {
            String line;
            while (true) {
                try {
                    line = reader.readLine("> ");
                } catch (EndOfFileException ignored) {
                    // Continue reading after EOT
                    continue;
                }

                if (line == null)
                    break;

                CommandHandler.executeCommand(line);
            }
        } catch (UserInterruptException e) {
            for (ServiceHolder<?> serviceHolder : ServiceHolderLookup.getAll()) {
                serviceHolder.shutdownIfPossible();
            }
        } finally {
            TerminalConsoleAppender.setReader(null);
        }
    }
}
