package com.aionemu.gameserver.dataholders.loadingutils;

import java.io.PrintStream;

import com.aionemu.gameserver.configs.main.GSConfig;

final class ConsoleStaticDataProgressReporter implements StaticDataProgressReporter {

	private static final int BAR_WIDTH = 20;
	private static final String SECTION_SEPARATOR = "────────────────────────────────────────────────────────";

	private final PrintStream out;
	private final boolean enabled;
	private int lastLineLength;

	ConsoleStaticDataProgressReporter(PrintStream out, boolean enabled) {
		this.out = out;
		this.enabled = enabled;
	}

	static StaticDataProgressReporter forCurrentConsole() {
		return new ConsoleStaticDataProgressReporter(System.out, GSConfig.STARTUP_PROGRESS_ENABLE);
	}

	@Override
	public void start(int totalSections) {
		if (!enabled) {
			return;
		}
		out.println(SECTION_SEPARATOR);
		out.println("Loading static data..");
		out.flush();
	}

	@Override
	public void sectionStarted(int sectionIndex, int totalSections, String sectionName, int totalEntries) {
		// Keep the console row clear until there is actual progress to render.
	}

	@Override
	public void sectionProgress(int sectionIndex, int totalSections, String sectionName, int currentEntries, int totalEntries) {
		if (!enabled) {
			return;
		}
		render(sectionLine(sectionName, currentEntries, totalEntries));
	}

	@Override
	public void sectionFinished(int sectionIndex, int totalSections, String sectionName, int totalEntries) {
		if (!enabled) {
			return;
		}
		render(sectionLine(sectionName, totalEntries, totalEntries));
		out.println();
		out.flush();
		lastLineLength = 0;
	}

	@Override
	public void finish(int totalSections, long elapsedMillis) {
		if (!enabled) {
			return;
		}
		out.printf("Loaded static data in %d ms%n", elapsedMillis);
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

	private String progressBar(int currentEntries, int totalEntries) {
		int boundedTotal = Math.max(1, totalEntries);
		int boundedCurrent = Math.min(boundedTotal, Math.max(0, currentEntries));
		int filled = boundedCurrent * BAR_WIDTH / boundedTotal;
		return "█".repeat(filled) + "░".repeat(BAR_WIDTH - filled);
	}

	private String sectionLine(String sectionName, int currentEntries, int totalEntries) {
		return String.format("%s | \"%s\" | %d/%d", progressBar(currentEntries, totalEntries), sectionName, currentEntries, totalEntries);
	}
}
