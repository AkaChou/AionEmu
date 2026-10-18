package com.aionemu.gameserver.dataholders.loadingutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import com.aionemu.testutil.ConfigSnapshot;
import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.configs.main.GSConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class ConsoleStaticDataProgressReporterTest {

	private final ConfigSnapshot gsConfigSnapshot = ConfigSnapshot.of(GSConfig.class,
		"STARTUP_PROGRESS_ENABLE", "STATIC_DATA_PROGRESS_ENTRY_COUNTS_ENABLE");

	private ListAppender<ILoggingEvent> appender;
	private Logger consoleLogger;
	private Level previousLevel;

	@BeforeEach
	void enableProgressEntryCounts() {
		GSConfig.STARTUP_PROGRESS_ENABLE = true;
		GSConfig.STATIC_DATA_PROGRESS_ENTRY_COUNTS_ENABLE = true;

		consoleLogger = (Logger) LoggerFactory.getLogger("aion.console");
		previousLevel = consoleLogger.getLevel();
		consoleLogger.setLevel(Level.DEBUG);
		appender = new ListAppender<>();
		appender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
		appender.start();
		consoleLogger.addAppender(appender);
	}

	@AfterEach
	void restoreOutputAndProperties() {
		consoleLogger.detachAppender(appender);
		appender.stop();
		consoleLogger.setLevel(previousLevel);
		gsConfigSnapshot.restore();
	}

	@Test
	void enabledReporterPrintsSectionTitleAtDebugWithoutDuplicatingElapsedTime() {
		ConsoleStaticDataProgressReporter reporter = new ConsoleStaticDataProgressReporter(true);

		reporter.start(3);
		reporter.sectionStarted(1, 3, "ItemData", 87);
		reporter.sectionProgress(1, 3, "ItemData", 41, 87);
		reporter.sectionFinished(1, 3, "ItemData", 87);
		reporter.finish(3, 1234);

		List<ILoggingEvent> events = appender.list;
		assertEquals(3, events.size());
		assertTrue(events.stream().allMatch(event -> event.getLevel() == Level.DEBUG));
		assertEquals("", events.get(0).getFormattedMessage());
		assertEquals("  " + I18n.get("console.static_data.loading"), events.get(1).getFormattedMessage());
		// 耗时由 DataManager 的权威结果行输出，报告器不再重复。 / Timing is owned by DataManager's authoritative result line.
		assertTrue(events.stream().noneMatch(event -> event.getFormattedMessage().contains("1234")));
		assertFalse(events.stream().anyMatch(event -> event.getFormattedMessage().contains("ItemData")));
	}

	@Test
	void sectionStartedDoesNotLeaveUnfinishedZeroProgressLine() {
		ConsoleStaticDataProgressReporter reporter = new ConsoleStaticDataProgressReporter(true);

		reporter.start(1);
		reporter.sectionStarted(1, 1, "MotionData", 333);

		assertEquals(3, appender.list.size());
		assertTrue(appender.list.stream().noneMatch(event -> event.getFormattedMessage().contains("MotionData")));
	}

	@Test
	void disabledReporterWritesNothing() {
		ConsoleStaticDataProgressReporter reporter = new ConsoleStaticDataProgressReporter(false);

		reporter.start(2);
		reporter.sectionStarted(1, 2, "ItemData", 1);
		reporter.sectionProgress(1, 2, "ItemData", 1, 1);
		reporter.sectionFinished(1, 2, "ItemData", 1);
		reporter.finish(2, 10);

		assertEquals(List.of(), appender.list);
	}

	@Test
	void currentConsoleReporterWritesNothingWhenEntryCountsAreEnabled() {
		StaticDataProgressReporter reporter = ConsoleStaticDataProgressReporter.forCurrentConsole();

		reporter.start(1);
		reporter.sectionStarted(1, 1, "ItemData", 1);
		reporter.sectionProgress(1, 1, "ItemData", 1, 1);
		reporter.sectionFinished(1, 1, "ItemData", 1);
		reporter.finish(1, 5);

		assertEquals(3, appender.list.size());
		assertTrue(appender.list.stream().allMatch(event -> event.getLevel() == Level.DEBUG));
	}

	@Test
	void disablePropertySuppressesCurrentConsoleReporter() {
		GSConfig.STARTUP_PROGRESS_ENABLE = false;
		StaticDataProgressReporter reporter = ConsoleStaticDataProgressReporter.forCurrentConsole();

		reporter.start(1);
		reporter.sectionStarted(1, 1, "ItemData", 1);
		reporter.sectionProgress(1, 1, "ItemData", 1, 1);
		reporter.sectionFinished(1, 1, "ItemData", 1);
		reporter.finish(1, 5);

		assertEquals(List.of(), appender.list);
	}
}
