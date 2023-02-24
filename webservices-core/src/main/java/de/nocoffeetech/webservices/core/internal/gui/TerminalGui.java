package de.nocoffeetech.webservices.core.internal.gui;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import de.nocoffeetech.webservices.core.terminal.CommandHandler;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class TerminalGui {
    private static final Logger LOGGER = LogManager.getLogger(TerminalGui.class);
    private static final Map<Level, Color> LOG_COLORS = new HashMap<>();

    static {
        // Adopted from HighlightConverter
        LOG_COLORS.put(Level.FATAL, Color.RED);
        LOG_COLORS.put(Level.ERROR, Color.RED);
        LOG_COLORS.put(Level.WARN, Color.YELLOW);
        LOG_COLORS.put(Level.INFO, Color.GREEN);
        LOG_COLORS.put(Level.DEBUG, Color.CYAN);
        LOG_COLORS.put(Level.TRACE, Color.WHITE);
    }

    private final JTextPane logPane = new JTextPane();
    private final JTextField commandField = new JTextField();


    public TerminalGui() {
        JFrame frame = new JFrame();
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                JDialog window = null;
                try {
                    window = (JDialog) e.getSource();
                } catch (ClassCastException ignored) {
                }
                if (JOptionPane.showConfirmDialog(window, "Are you sure you want to shut down the server?", "Confirm Shutdown", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    GuiLogAppender.deactivate();
                    System.exit(0); // TODO proper shutdown
                }
            }
        });
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(854, 480));
        panel.setLayout(new BorderLayout());
        frame.setContentPane(panel);

        logPane.setBackground(Color.BLACK);
        logPane.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logPane, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel southPanel = new JPanel();
        southPanel.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 98;
        commandField.setFocusTraversalKeysEnabled(false);
        commandField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();
                if (keyCode == KeyEvent.VK_RIGHT || keyCode == KeyEvent.VK_KP_RIGHT || keyCode == KeyEvent.VK_TAB) {
                    completeCommand();
                } else if (keyCode == KeyEvent.VK_ENTER) {
                    sendCommand();
                }
            }
        });
        southPanel.add(commandField, constraints);

        JButton suggestBtn = new JButton("suggest");
        suggestBtn.addActionListener(e -> this.completeCommand());
        constraints.gridx = 1;
        constraints.weightx = 1;
        southPanel.add(suggestBtn, constraints);

        JButton sendBtn = new JButton("send");
        sendBtn.addActionListener(e -> this.sendCommand());
        constraints.gridx = 2;
        constraints.weightx = 1;
        southPanel.add(sendBtn, constraints);

        panel.add(southPanel, BorderLayout.SOUTH);

        frame.setTitle("Nocoffeetech Webservices Framework");
        frame.setMinimumSize(new Dimension(854 / 2, 480 / 2));
        frame.pack();
        frame.setVisible(true);


        Thread pollerThread = new Thread(() -> {
            try {
                while (GuiLogAppender.isActive()) {
                    LogMessageContext context = GuiLogAppender.awaitNext();
                    appendContext(context);
                }
            } catch (InterruptedException e) {
                LOGGER.warn("Interrupted log pulling!", e);
                appendContext(new LogMessageContext("Interrupted log pulling!", Level.WARN));
            }
        });
        pollerThread.setDaemon(true);
        pollerThread.setPriority(3);
        pollerThread.start();
    }

    private void sendCommand() {
        String text = commandField.getText();
        LOGGER.info("Running command {}", text);
        CommandHandler.executeCommand(text);
        commandField.setText("");
    }

    private void completeCommand() {
        String line = commandField.getText();
        if (line.isEmpty() || line.charAt(0) != '/')
        {
            line = '/' + line;
        }

        StringReader stringReader = new StringReader(line);
        if (stringReader.canRead() && stringReader.peek() == '/')
            stringReader.skip();

        try
        {
            ParseResults<Void> results = CommandHandler.DISPATCHER.parse(stringReader, null);
            Suggestions tabComplete = CommandHandler.DISPATCHER.getCompletionSuggestions(results).get();
            List<Suggestion> suggestions = tabComplete.getList();
            StringBuilder suggestionBuilder = new StringBuilder();
            for (Suggestion suggestion : suggestions)
            {
                String completion = suggestion.getText();
                if (!completion.isEmpty())
                {
                    if (suggestions.size() == 1) {
                        commandField.setText(suggestion.apply(line));
                    } else {
                        suggestionBuilder.append(completion).append(", ");
                    }
                }
            }
            if (!suggestionBuilder.isEmpty()) {
                suggestionBuilder.delete(suggestionBuilder.length() - 2, suggestionBuilder.length() - 1);
                suggestionBuilder.append('\n');
                appendContext(new LogMessageContext(suggestionBuilder.toString(), null));
            }
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Error completing command!", e);
        }
    }

    private void appendContext(LogMessageContext context) {
        SwingUtilities.invokeLater(() -> {
            Document document = logPane.getDocument();

            try {
                SimpleAttributeSet simpleAttributeSet = new SimpleAttributeSet(logPane.getInputAttributes());
                Level level = context.level();
                StyleConstants.setForeground(simpleAttributeSet, LOG_COLORS.getOrDefault(level, Color.WHITE));
                document.insertString(document.getLength(), context.text(), simpleAttributeSet);
            } catch (BadLocationException badlocationexception) {
                throw new RuntimeException(badlocationexception);
            }

        });
    }
}
