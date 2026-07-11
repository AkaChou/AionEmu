package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import com.aionemu.boot.i18n.I18n;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;

class ConsoleStartupProgressReporterTest {
	@BeforeEach
	void setUpMessages() {
		StaticMessageSource source = new StaticMessageSource();
		source.addMessage("console.progress.loading_group", Locale.ENGLISH, "Loading {0}..");
		source.addMessage("console.progress.loaded_group", Locale.ENGLISH, "Loaded {0} in {1} ms");
		I18n.setMessageSource(source);
		I18n.applyCountryCode(1);
	}

	@AfterEach
	void clearMessages() {
		I18n.setMessageSource(null);
	}

	@Test
	void enabledReporterPrintsYarnStyleStepBarsWithCurrentAndTotal() {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		ConsoleStartupProgressReporter reporter = new ConsoleStartupProgressReporter(
			new PrintStream(bytes, true, StandardCharsets.UTF_8),
			true
		);

		reporter.start("game world");
		reporter.stepStarted("IDFactory");
		reporter.stepFinished("IDFactory");
		reporter.stepStarted("Zone");
		reporter.stepFinished("Zone");
		reporter.finish("game world", 123);

		String output = bytes.toString(StandardCharsets.UTF_8);
		assertTrue(output.startsWith("────────────────────────────────────────────────────────\nLoading game world..\n"));
		assertTrue(output.contains("\r████████████████████ | \"IDFactory\" | 1/1\n"));
		assertTrue(output.contains("\r████████████████████ | \"Zone\" | 1/1\n"));
		assertTrue(output.contains("Loaded game world in 123 ms\n"));
		assertFalse(output.contains("\"IDFactory\" | 0/1"));
		assertFalse(output.contains("\"Zone\" | 0/1"));
		assertFalse(output.contains("%"));
		assertEquals(5, output.chars().filter(character -> character == '\n').count());
	}

	@Test
	void stepStartedDoesNotLeaveUnfinishedZeroProgressLine() {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		ConsoleStartupProgressReporter reporter = new ConsoleStartupProgressReporter(
			new PrintStream(bytes, true, StandardCharsets.UTF_8),
			true
		);

		reporter.start("game world");
		reporter.stepStarted("IDFactory");

		String output = bytes.toString(StandardCharsets.UTF_8);
		assertTrue(output.endsWith("Loading game world..\n"));
		assertFalse(output.contains("IDFactory"));
		assertFalse(output.contains("\r"));
	}

	@Test
	void disabledReporterWritesNothing() {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		ConsoleStartupProgressReporter reporter = new ConsoleStartupProgressReporter(
			new PrintStream(bytes, true, StandardCharsets.UTF_8),
			false
		);

		reporter.start("game world");
		reporter.stepStarted("IDFactory");
		reporter.stepFinished("IDFactory");
		reporter.finish("game world", 1);

		assertEquals("", bytes.toString(StandardCharsets.UTF_8));
	}
}
