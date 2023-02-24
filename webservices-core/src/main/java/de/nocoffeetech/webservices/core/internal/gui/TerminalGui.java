package de.nocoffeetech.webservices.core.internal.gui;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.pattern.AnsiEscape;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;

public class TerminalGui {
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

        JTextPane textPane = new JTextPane();
        textPane.setBackground(Color.BLACK);
        textPane.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textPane, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel southPanel = new JPanel();
        southPanel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 99;
        JTextField textField = new JTextField();
        southPanel.add(textField, constraints);

        // TODO command handling
        JButton sendBtn = new JButton("send");
        constraints.gridx = 1;
        constraints.weightx = 1;
        southPanel.add(sendBtn, constraints);

        panel.add(southPanel, BorderLayout.SOUTH);

        frame.setTitle("Nocoffeetech Webservices Framework");
        frame.setMinimumSize(new Dimension(854 / 2, 480 / 2));
        frame.pack();
        frame.setVisible(true);


        Thread pollerThread = new Thread(() -> {
            try {
                while (true) {
                    LogMessageContext context = GuiLogAppender.awaitNext();
                    SwingUtilities.invokeLater(() -> {
                        Document document = textPane.getDocument();

                        try {
                            SimpleAttributeSet simpleAttributeSet = new SimpleAttributeSet(textPane.getInputAttributes());
                            Level level = context.level();
                            StyleConstants.setForeground(simpleAttributeSet, LOG_COLORS.getOrDefault(level, Color.WHITE));
                            document.insertString(document.getLength(), context.text(), simpleAttributeSet);
                        } catch (BadLocationException badlocationexception) {
                            throw new RuntimeException(badlocationexception);
                        }

                    });
                }
            } catch (InterruptedException e) {
                // TODO
            }
        });
        pollerThread.setDaemon(true);
        pollerThread.setPriority(3);
        pollerThread.start();
    }
}
