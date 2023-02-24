package de.nocoffeetech.webservices.core.internal.gui;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.AbstractOutputStreamAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;

import java.io.Serializable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Plugin(name = "GuiLog", category = "Core", elementType = "appender")
public class GuiLogAppender extends AbstractAppender {
    private static final BlockingQueue<LogMessageContext> messageQueue = new LinkedBlockingQueue<>();
    private static final int MAX_MESSAGES = 100;
    private static boolean active = true;

    static void deactivate() {
        active = false;
        messageQueue.clear();
    }

    static boolean isActive() {
        return active;
    }

    static LogMessageContext awaitNext() throws InterruptedException {
        return messageQueue.take();
    }

    protected GuiLogAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions, Property[] properties) {
        super(name, filter, layout, ignoreExceptions, properties);
    }

    @Override
    public void append(LogEvent event) {
        if (!active) return;
        String message = getLayout().toSerializable(event).toString();
        if (messageQueue.size() > MAX_MESSAGES)
            messageQueue.poll();
        messageQueue.add(new LogMessageContext(message, event.getLevel()));
    }

    @PluginBuilderFactory
    public static <B extends Builder<B>> B newBuilder() {
        return new Builder<B>().asBuilder();
    }

    public static class Builder<B extends Builder<B>> extends AbstractOutputStreamAppender.Builder<B> implements org.apache.logging.log4j.core.util.Builder<GuiLogAppender> {
        @Override
        public GuiLogAppender build() {
            if (!isValid()) {
                return null;
            }
            return new GuiLogAppender(getName(), getFilter(), getLayout(), isIgnoreExceptions(), getPropertyArray());
        }
    }
}
