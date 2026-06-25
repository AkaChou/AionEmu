package com.aionemu.gameserver.lifecycle;

import java.io.PrintStream;

import com.aionemu.gameserver.configs.main.GSConfig;
import com.aionemu.gameserver.utils.ConsoleProgressLineRenderer;

final class ConsoleStartupProgressReporter implements StartupProgressReporter {

	private static final String SECTION_SEPARATOR = "────────────────────────────────────────────────────────";

	private final PrintStream out;
	private final boolean enabled;
	private final ConsoleProgressLineRenderer progressRenderer;

	ConsoleStartupProgressReporter(PrintStream out, boolean enabled) {
		this.out = out;
		this.enabled = enabled;
		this.progressRenderer = new ConsoleProgressLineRenderer(out, enabled);
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
		progressRenderer.finished(stepName, 1);
	}

	@Override
	public void finish(String groupName, long elapsedMillis) {
		if (!enabled) {
			return;
		}
		out.printf("Loaded %s in %d ms%n", groupName, elapsedMillis);
		out.flush();
	}

	@Override
	public void failed() {
		progressRenderer.clearLine();
	}
}
