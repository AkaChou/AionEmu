package com.aionemu.commons.utils.concurrent;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class ExecuteWrapperTest {

    @Test
    void logsRunnableClassWhenExecutionExceedsWarningThreshold() {
        Logger logger = (Logger) LoggerFactory.getLogger(ExecuteWrapper.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        boolean additive = logger.isAdditive();
        logger.setAdditive(false);
        logger.addAppender(appender);
        AtomicBoolean ran = new AtomicBoolean();

        try {
            ExecuteWrapper.execute(new NamedRunnable(ran), -1);
        } finally {
            logger.detachAppender(appender);
            logger.setAdditive(additive);
            appender.stop();
        }

        assertTrue(ran.get());
        assertTrue(appender.list.stream()
            .map(ILoggingEvent::getFormattedMessage)
            .anyMatch(message -> message.contains(NamedRunnable.class.getName()) && message.contains("执行时间")));
    }

    private record NamedRunnable(AtomicBoolean ran) implements Runnable {

        @Override
        public void run() {
            ran.set(true);
        }
    }
}
