package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import com.aionemu.boot.i18n.I18n;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.StaticMessageSource;

class ConsoleStartupProgressReporterTest {

	private ListAppender<ILoggingEvent> appender;
	private Logger consoleLogger;

	@BeforeEach
	void setUpMessages() {
		StaticMessageSource source = new StaticMessageSource();
		source.addMessage("console.progress.loading_group", Locale.ENGLISH, "Loading {0}..");
		source.addMessage("console.progress.loaded_group", Locale.ENGLISH, "Loaded {0} in {1} ms");
		I18n.setMessageSource(source);
		I18n.applyCountryCode(1);

		consoleLogger = (Logger) LoggerFactory.getLogger("aion.console");
		appender = new ListAppender<>();
		appender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
		appender.start();
		consoleLogger.addAppender(appender);
	}

	@AfterEach
	void clearMessages() {
		consoleLogger.detachAppender(appender);
		appender.stop();
		I18n.setMessageSource(null);
	}

	@Test
	void enabledReporterPrintsSectionAndSummaryWithoutProgressBars() {
		ConsoleStartupProgressReporter reporter = new ConsoleStartupProgressReporter(true);

		reporter.start("game world");
		reporter.stepStarted("IDFactory");
		reporter.stepFinished("IDFactory");
		reporter.stepStarted("Zone");
		reporter.stepFinished("Zone");
		reporter.finish("game world", 123);

		List<ILoggingEvent> events = appender.list;
		assertEquals(4, events.size());
		assertEquals("", events.get(0).getFormattedMessage());
		assertEquals("  Loading game world..", events.get(1).getFormattedMessage());
		assertEquals("  " + "─".repeat(46), events.get(2).getFormattedMessage());
		assertEquals("Loaded game world in 123 ms", events.get(3).getFormattedMessage());
		// 进度条已移除：不含方块字符、回车覆盖或逐步骤进度行。 / Progress bar removed: no block chars, carriage returns, or per-step progress lines.
		String output = events.stream().map(ILoggingEvent::getFormattedMessage).reduce("", (a, b) -> a + b);
		assertFalse(output.contains("█"));
		assertFalse(output.contains("░"));
		assertFalse(output.contains("\r"));
		assertFalse(output.contains("\"IDFactory\""));
		assertFalse(output.contains("\"Zone\""));
	}

	@Test
	void stepStartedDoesNotLeaveUnfinishedZeroProgressLine() {
		ConsoleStartupProgressReporter reporter = new ConsoleStartupProgressReporter(true);

		reporter.start("game world");
		reporter.stepStarted("IDFactory");

		assertEquals(3, appender.list.size());
		assertEquals("  Loading game world..", appender.list.get(1).getFormattedMessage());
		assertTrue(appender.list.stream().noneMatch(event -> event.getFormattedMessage().contains("IDFactory")));
	}

	@Test
	void disabledReporterWritesNothing() {
		ConsoleStartupProgressReporter reporter = new ConsoleStartupProgressReporter(false);

		reporter.start("game world");
		reporter.stepStarted("IDFactory");
		reporter.stepFinished("IDFactory");
		reporter.finish("game world", 1);

		assertEquals(List.of(), appender.list);
	}
}
