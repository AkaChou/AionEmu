package com.aionemu.commons.versionning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class VersionTest {

    @Test
    void explodedClassDirectoryUsesUnknownVersionWithoutWarning() {
        Logger logger = (Logger) LoggerFactory.getLogger(Version.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        boolean additive = logger.isAdditive();
        logger.setAdditive(false);
        logger.addAppender(appender);
        Version version = new Version();

        try {
            version.loadInformation(Version.class);
        } finally {
            logger.detachAppender(appender);
            logger.setAdditive(additive);
            appender.stop();
        }

        assertEquals("Unknown Revision", version.getRevision());
        assertFalse(appender.list.stream()
            .map(ILoggingEvent::getFormattedMessage)
            .anyMatch(message -> message.contains("Unable to get Soft information")));
    }
}
