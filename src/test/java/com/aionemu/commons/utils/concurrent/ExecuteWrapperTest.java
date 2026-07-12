package com.aionemu.commons.utils.concurrent;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import com.aionemu.boot.i18n.I18n;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ResourceBundleMessageSource;

class ExecuteWrapperTest {

    @Test
    void logsSlowRunnableAndFailureWithThrowable() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        I18n.setMessageSource(messageSource);
        I18n.applyCountryCode(1);
        Logger logger = (Logger) LoggerFactory.getLogger(ExecuteWrapper.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        boolean additive = logger.isAdditive();
        logger.setAdditive(false);
        logger.addAppender(appender);
        AtomicBoolean ran = new AtomicBoolean();
        try {
            ExecuteWrapper.execute(new NamedRunnable(ran), -1);
            ExecuteWrapper.execute(() -> {
                throw new IllegalStateException("boom");
            }, Long.MAX_VALUE);
        } finally {
            logger.detachAppender(appender);
            logger.setAdditive(additive);
            appender.stop();
            I18n.setMessageSource(null);
        }

        assertTrue(ran.get());
        assertTrue(appender.list.size() >= 2);
        assertTrue(appender.list.stream()
            .map(ILoggingEvent::getFormattedMessage)
            .anyMatch(message -> message.contains(NamedRunnable.class.getName())));
        assertTrue(appender.list.stream()
            .anyMatch(event -> event.getThrowableProxy() != null
                && IllegalStateException.class.getName().equals(event.getThrowableProxy().getClassName())));
    }

    private record NamedRunnable(AtomicBoolean ran) implements Runnable {

        @Override
        public void run() {
            ran.set(true);
        }
    }
}
