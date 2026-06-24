package com.aionemu.gameserver.dataholders.loadingutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import com.aionemu.gameserver.configs.main.GSConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ConsoleStaticDataProgressReporterTest {

	private final PrintStream originalOut = System.out;

	@AfterEach
	void restoreOutputAndProperties() {
		System.setOut(originalOut);
		GSConfig.STARTUP_PROGRESS_ENABLE = true;
	}

	@Test
	void enabledReporterPrintsOneBlockProgressLinePerSection() {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		ConsoleStaticDataProgressReporter reporter = new ConsoleStaticDataProgressReporter(
			new PrintStream(bytes, true, StandardCharsets.UTF_8),
			true
		);

		reporter.start(3);
		reporter.sectionStarted(1, 3, "ItemData", 87);
		reporter.sectionProgress(1, 3, "ItemData", 41, 87);
		reporter.sectionFinished(1, 3, "ItemData", 87);
		reporter.sectionStarted(2, 3, "NpcDropData", 5342);
		reporter.sectionProgress(2, 3, "NpcDropData", 41, 5342);
		reporter.sectionFinished(2, 3, "NpcDropData", 5342);
		reporter.finish(3, 1234);

		String output = bytes.toString(StandardCharsets.UTF_8);
		assertTrue(output.startsWith("────────────────────────────────────────────────────────\nLoading static data..\n"));
		assertTrue(output.contains("\r█████████░░░░░░░░░░░ | \"ItemData\" | 41/87"));
		assertTrue(output.contains("\r████████████████████ | \"ItemData\" | 87/87\n"));
		assertTrue(output.contains("\r████████████████████ | \"NpcDropData\" | 5342/5342\n"));
		assertTrue(output.contains("Loaded static data in 1234 ms\n"));
		assertFalse(output.contains("\"ItemData\" | 0/87"));
		assertFalse(output.contains("\"NpcDropData\" | 0/5342"));
		assertFalse(output.contains("%"));
		assertFalse(output.contains("[1/3]"));
		assertEquals(5, output.chars().filter(character -> character == '\n').count());
	}

	@Test
	void sectionStartedDoesNotLeaveUnfinishedZeroProgressLine() {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		ConsoleStaticDataProgressReporter reporter = new ConsoleStaticDataProgressReporter(
			new PrintStream(bytes, true, StandardCharsets.UTF_8),
			true
		);

		reporter.start(1);
		reporter.sectionStarted(1, 1, "MotionData", 333);

		String output = bytes.toString(StandardCharsets.UTF_8);
		assertTrue(output.endsWith("Loading static data..\n"));
		assertFalse(output.contains("MotionData"));
		assertFalse(output.contains("\r"));
	}

	@Test
	void disabledReporterDoesNotWriteControlCharactersToOutput() {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		ConsoleStaticDataProgressReporter reporter = new ConsoleStaticDataProgressReporter(
			new PrintStream(bytes, true, StandardCharsets.UTF_8),
			false
		);

		reporter.start(2);
		reporter.sectionStarted(1, 2, "ItemData", 1);
		reporter.sectionProgress(1, 2, "ItemData", 1, 1);
		reporter.sectionFinished(1, 2, "ItemData", 1);
		reporter.finish(2, 10);

		assertFalse(bytes.toString(StandardCharsets.UTF_8).contains("\r"));
		assertEquals("", bytes.toString(StandardCharsets.UTF_8));
	}

	@Test
	void currentConsoleReporterWritesProgressByDefaultInIdeStyleConsoles() {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		System.setOut(new PrintStream(bytes, true, StandardCharsets.UTF_8));

		StaticDataProgressReporter reporter = ConsoleStaticDataProgressReporter.forCurrentConsole();

		reporter.start(1);
		reporter.sectionStarted(1, 1, "ItemData", 1);
		reporter.sectionProgress(1, 1, "ItemData", 1, 1);
		reporter.sectionFinished(1, 1, "ItemData", 1);
		reporter.finish(1, 5);

		assertTrue(bytes.toString(StandardCharsets.UTF_8).contains("\r████████████████████ | \"ItemData\" | 1/1"));
	}

	@Test
	void disablePropertySuppressesCurrentConsoleReporter() {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		System.setOut(new PrintStream(bytes, true, StandardCharsets.UTF_8));
		GSConfig.STARTUP_PROGRESS_ENABLE = false;

		StaticDataProgressReporter reporter = ConsoleStaticDataProgressReporter.forCurrentConsole();

		reporter.start(1);
		reporter.sectionStarted(1, 1, "ItemData", 1);
		reporter.sectionProgress(1, 1, "ItemData", 1, 1);
		reporter.sectionFinished(1, 1, "ItemData", 1);
		reporter.finish(1, 5);

		assertEquals("", bytes.toString(StandardCharsets.UTF_8));
	}
}
