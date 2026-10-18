package com.aionemu.commons.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class AEInfosTest {

	private ListAppender<ILoggingEvent> appender;
	private Logger aeInfosLogger;

	@BeforeEach
	void attachListAppender() {
		aeInfosLogger = (Logger) LoggerFactory.getLogger(AEInfos.class);
		appender = new ListAppender<>();
		appender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
		appender.start();
		aeInfosLogger.addAppender(appender);
		AEInfos.resetPrintedStateForTest();
	}

	@AfterEach
	void detachListAppender() {
		aeInfosLogger.detachAppender(appender);
		appender.stop();
		AEInfos.resetPrintedStateForTest();
	}

	@Test
	void memoryInfoHasNoBoxDecorations() {
		for (String line : AEInfos.getMemoryInfo()) {
			assertFalse(line.startsWith("+----"), line);
			assertNotEquals("|    |", line, line);
			assertFalse(line.contains("...."), line);
		}
		assertTrue(AEInfos.getMemoryInfo().length >= 4);
	}

	@Test
	void cpuOsJreJvmHaveNoDotRules() {
		for (String[] block : new String[][] {
			AEInfos.getCPUInfo(), AEInfos.getOSInfo(), AEInfos.getJREInfo(), AEInfos.getJVMInfo()
		}) {
			for (String line : block) {
				assertFalse(line.startsWith("...."), line);
			}
			assertTrue(block.length >= 1);
		}
	}

	@Test
	void printAllInfosOnlyPrintsOnFirstCall() {
		int linesPerBlock = AEInfos.getOSInfo().length
			+ AEInfos.getCPUInfo().length
			+ AEInfos.getJREInfo().length
			+ AEInfos.getJVMInfo().length
			+ AEInfos.getMemoryInfo().length;

		AEInfos.printAllInfos();
		assertEquals(linesPerBlock, appender.list.size());

		List<ILoggingEvent> firstBlock = List.copyOf(appender.list);
		AEInfos.printAllInfos();
		AEInfos.printAllInfos();

		assertEquals(firstBlock.size(), appender.list.size());
	}
}
