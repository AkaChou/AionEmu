package com.aionemu.commons.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.spi.FilterReply;
import com.aionemu.commons.logging.slf4j.filters.AuditFilter;
import com.aionemu.commons.logging.slf4j.filters.ChatLogFilter;
import com.aionemu.commons.logging.slf4j.filters.ConsoleFilter;
import com.aionemu.commons.logging.slf4j.filters.GmAuditFilter;
import com.aionemu.commons.logging.slf4j.filters.ItemFilter;
import org.junit.jupiter.api.Test;

class LogTagsFilterTest {

    @Test
    void constantsMatchHistoricalPrefixes() {
        assertEquals("[MESSAGE]", LogTags.MESSAGE);
        assertEquals("[ITEM]", LogTags.ITEM);
        assertEquals("[ADMIN COMMAND]", LogTags.ADMIN);
        assertEquals("[AUDIT]", LogTags.AUDIT);
    }

    @Test
    void consoleFilterDeniesTaggedMessages() {
        ConsoleFilter filter = new ConsoleFilter();
        assertEquals(FilterReply.DENY, filter.decide(event(LogTags.MESSAGE + " hi")));
        assertEquals(FilterReply.DENY, filter.decide(event(LogTags.ITEM + " hi")));
        assertEquals(FilterReply.DENY, filter.decide(event(LogTags.ADMIN + " hi")));
        assertEquals(FilterReply.DENY, filter.decide(event(LogTags.AUDIT + " hi")));
        assertEquals(FilterReply.ACCEPT, filter.decide(event("normal startup line")));
    }

    @Test
    void dedicatedFiltersAcceptOnlyTheirTag() {
        assertEquals(FilterReply.ACCEPT, new ChatLogFilter().decide(event(LogTags.MESSAGE + " x")));
        assertEquals(FilterReply.DENY, new ChatLogFilter().decide(event("x")));
        assertEquals(FilterReply.ACCEPT, new ItemFilter().decide(event(LogTags.ITEM + " x")));
        assertEquals(FilterReply.ACCEPT, new GmAuditFilter().decide(event(LogTags.ADMIN + " x")));
        assertEquals(FilterReply.ACCEPT, new AuditFilter().decide(event(LogTags.AUDIT + " x")));
    }

    private static LoggingEvent event(String message) {
        LoggingEvent e = new LoggingEvent();
        e.setLevel(Level.INFO);
        e.setMessage(message);
        return e;
    }
}
