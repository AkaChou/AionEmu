package com.aionemu.gameserver.dataholders.loadingutils;

import java.io.PrintStream;

import com.aionemu.gameserver.configs.main.GSConfig;
import com.aionemu.gameserver.utils.ConsoleProgressLineRenderer;

final class ConsoleStaticDataProgressReporter implements StaticDataProgressReporter {

	private static final String SECTION_SEPARATOR = "────────────────────────────────────────────────────────";

	private final PrintStream out;
	private final boolean enabled;
	private final ConsoleProgressLineRenderer progressRenderer;

	ConsoleStaticDataProgressReporter(PrintStream out, boolean enabled) {
		this.out = out;
		this.enabled = enabled;
		this.progressRenderer = new ConsoleProgressLineRenderer(out, enabled);
	}

	static StaticDataProgressReporter forCurrentConsole() {
		return new ConsoleStaticDataProgressReporter(System.out, GSConfig.STARTUP_PROGRESS_ENABLE);
	}

	@Override
	public void start(int totalSections) {
		if (!progressEnabled()) {
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
		if (!progressEnabled()) {
			return;
		}
		progressRenderer.progress(sectionName, currentEntries, totalEntries);
	}

	@Override
	public void sectionFinished(int sectionIndex, int totalSections, String sectionName, int totalEntries) {
		if (!progressEnabled()) {
			return;
		}
		progressRenderer.finished(sectionName, totalEntries);
	}

	@Override
	public void finish(int totalSections, long elapsedMillis) {
		if (!enabled) {
			return;
		}
		out.printf("Loaded static data in %d ms%n", elapsedMillis);
		out.flush();
	}

	@Override
	public void failed() {
		if (!progressEnabled()) {
			return;
		}
		progressRenderer.clearLine();
	}

	private boolean progressEnabled() {
		return enabled && GSConfig.STATIC_DATA_PROGRESS_ENTRY_COUNTS_ENABLE;
	}
}
