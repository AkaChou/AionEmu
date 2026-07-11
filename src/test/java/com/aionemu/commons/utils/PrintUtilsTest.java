package com.aionemu.commons.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class PrintUtilsTest {

    private ListAppender<ILoggingEvent> appender;
    private Logger consoleLogger;

    @BeforeEach
    void attachListAppender() {
        consoleLogger = (Logger) LoggerFactory.getLogger("aion.console");
        appender = new ListAppender<>();
        appender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        appender.start();
        consoleLogger.addAppender(appender);
    }

    @AfterEach
    void detachListAppender() {
        consoleLogger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void normalizeTitleStripsLegacyDecorations() {
        assertEquals("Config", PrintUtils.normalizeTitle(" *** Config *** "));
        assertEquals("Network", PrintUtils.normalizeTitle("-[ Network ]"));
        assertEquals("X", PrintUtils.normalizeTitle("[ X ]"));
        assertEquals("", PrintUtils.normalizeTitle(null));
        assertEquals("", PrintUtils.normalizeTitle("   ***   "));
        assertEquals("Static Data", PrintUtils.normalizeTitle(" *** Static   Data *** "));
    }

    @Test
    void printSectionEmitsBlankTitleAndRule() {
        PrintUtils.printSection(" *** Config *** ");

        List<ILoggingEvent> events = appender.list;
        assertEquals(3, events.size());
        assertEquals("", events.get(0).getFormattedMessage());
        assertEquals("  Config", events.get(1).getFormattedMessage());
        assertEquals("  " + "─".repeat(46), events.get(2).getFormattedMessage());
    }

    @Test
    void printSubSectionEmitsBullet() {
        PrintUtils.printSubSection("detail");
        assertEquals(1, appender.list.size());
        assertEquals("  · detail", appender.list.get(0).getFormattedMessage());
    }

    @Test
    void emptyTitleStillPrintsRule() {
        PrintUtils.printSection("***");
        assertTrue(appender.list.stream().anyMatch(e -> e.getFormattedMessage().startsWith("  ─")));
    }
}
