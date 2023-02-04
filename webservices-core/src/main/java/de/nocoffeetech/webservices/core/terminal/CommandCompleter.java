package de.nocoffeetech.webservices.core.terminal;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class CommandCompleter implements Completer
{
    private static final Logger LOGGER = LogManager.getLogger(CommandCompleter.class);

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates)
    {
        String buffer = line.line();
        boolean prefix;
        if (buffer.isEmpty() || buffer.charAt(0) != '/')
        {
            buffer = '/' + buffer;
            prefix = false;
        }
        else
        {
            prefix = true;
        }

        StringReader stringReader = new StringReader(buffer);
        if (stringReader.canRead() && stringReader.peek() == '/')
            stringReader.skip();

        try
        {
            ParseResults<Void> results = CommandHandler.DISPATCHER.parse(stringReader, null);
            Suggestions tabComplete = CommandHandler.DISPATCHER.getCompletionSuggestions(results).get();
            for (Suggestion suggestion : tabComplete.getList())
            {
                String completion = suggestion.getText();
                if (!completion.isEmpty())
                {
                    boolean hasPrefix = prefix || completion.charAt(0) != '/';
                    Candidate candidate = new Candidate(hasPrefix ? completion : completion.substring(1));
                    candidates.add(candidate);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Error completing command!", e);
        }
    }

}
