package com.aionemu.gameserver.lifecycle;

import java.io.PrintStream;

import com.aionemu.gameserver.configs.main.GSConfig;

final class ConsoleStartupProgressReporter implements StartupProgressReporter {

	private static final int BAR_WIDTH = 20;
	private static final String SECTION_SEPARATOR = "────────────────────────────────────────────────────────";

	private final PrintStream out;
	private final boolean enabled;
	private int lastLineLength;

	ConsoleStartupProgressReporter(PrintStream out, boolean enabled) {
		this.out = out;
		this.enabled = enabled;
	}

	static StartupProgressReporter forCurrentConsole() {
		return new ConsoleStartupProgressReporter(System.out, GSConfig.STARTUP_PROGRESS_ENABLE);
	}

	@Override
	public void start(String groupName) {
		if (!enabled) {
			return;
		}
		out.println(SECTION_SEPARATOR);
		out.printf("Loading %s..%n", groupName);
		out.flush();
	}

	@Override
	public void stepStarted(String stepName) {
		// Keep the console row clear until the step has completed.
	}

	@Override
	public void stepFinished(String stepName) {
		if (!enabled) {
			return;
		}
		render(stepLine(stepName, 1, 1));
		out.println();
		out.flush();
		lastLineLength = 0;
	}

	@Override
	public void finish(String groupName, long elapsedMillis) {
		if (!enabled) {
			return;
		}
		out.printf("Loaded %s in %d ms%n", groupName, elapsedMillis);
		out.flush();
		lastLineLength = 0;
	}

	@Override
	public void failed() {
		if (!enabled) {
			return;
		}
		out.println();
		out.flush();
		lastLineLength = 0;
	}

	private void render(String line) {
		out.print('\r');
		out.print(line);
		int padding = lastLineLength - line.length();
		if (padding > 0) {
			out.print(" ".repeat(padding));
		}
		out.flush();
		lastLineLength = line.length();
	}

	private String progressBar(int current, int total) {
		int boundedTotal = Math.max(1, total);
		int boundedCurrent = Math.min(boundedTotal, Math.max(0, current));
		int filled = boundedCurrent * BAR_WIDTH / boundedTotal;
		return "█".repeat(filled) + "░".repeat(BAR_WIDTH - filled);
	}

	private String stepLine(String stepName, int current, int total) {
		return String.format("%s | \"%s\" | %d/%d", progressBar(current, total), stepName, current, total);
	}
}
